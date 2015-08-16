package org.greencheek.caching.herdcache.memcached.spy.extensions.locator;

import net.spy.memcached.*;
import net.spy.memcached.compat.SpyObject;
import net.spy.memcached.util.DefaultKetamaNodeLocatorConfiguration;
import net.spy.memcached.util.KetamaNodeLocatorConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.*;

/**
 *
 */
public class CeilingKeyKetamaNodeLocator extends SpyObject implements NodeLocator {

    private static Logger logger = LoggerFactory.getLogger(CeilingKeyKetamaNodeLocator.class);

    private volatile ArrayBasedCeilRing ketamaNodes;

    private final HashAlgorithm hashAlg;
    private final Map<InetSocketAddress, Integer> weights;
    private final boolean isWeightedKetama;
    private final KetamaNodeLocatorConfiguration config;

    /**
     * Create a new KetamaNodeLocator using specified nodes and the specifed hash
     * algorithm.
     *
     * @param nodes The List of nodes to use in the Ketama consistent hash
     *          continuum
     * @param alg The hash algorithm to use when choosing a node in the Ketama
     *          consistent hash continuum
     */
    public CeilingKeyKetamaNodeLocator(List<MemcachedNode> nodes, HashAlgorithm alg) {
        this(nodes, alg, KetamaNodeKeyFormatter.Format.SPYMEMCACHED, new HashMap<InetSocketAddress, Integer>());
    }

    /**
     * Create a new KetamaNodeLocator with specific nodes, hash, node key format,
     * and weight
     *
     * @param nodes The List of nodes to use in the Ketama consistent hash
     *          continuum
     * @param alg The hash algorithm to use when choosing a node in the Ketama
     *          consistent hash continuum
     * @param nodeKeyFormat the format used to name the nodes in Ketama, either
     *          SPYMEMCACHED or LIBMEMCACHED
     * @param weights node weights for ketama, a map from InetSocketAddress to
     *          weight as Integer
     */
    public CeilingKeyKetamaNodeLocator(List<MemcachedNode> nodes, HashAlgorithm alg,
                                       KetamaNodeKeyFormatter.Format nodeKeyFormat,
                                       Map<InetSocketAddress, Integer> weights) {
        this(nodes, alg, weights, new DefaultKetamaNodeLocatorConfiguration(new KetamaNodeKeyFormatter(nodeKeyFormat)));
    }

    /**
     * Create a new KetamaNodeLocator using specified nodes and the specifed hash
     * algorithm and configuration.
     *
     * @param nodes The List of nodes to use in the Ketama consistent hash
     *          continuum
     * @param alg The hash algorithm to use when choosing a node in the Ketama
     *          consistent hash continuum
     * @param conf
     */
    public CeilingKeyKetamaNodeLocator(List<MemcachedNode> nodes, HashAlgorithm alg,
                                       KetamaNodeLocatorConfiguration conf) {
        this(nodes, alg, new HashMap<InetSocketAddress, Integer>(), conf);
    }

    /**
     * Create a new KetamaNodeLocator with specific nodes, hash, node key format,
     * and weight
     *
     * @param nodes The List of nodes to use in the Ketama consistent hash
     *          continuum
     * @param alg The hash algorithm to use when choosing a node in the Ketama
     *          consistent hash continuum
     * @param nodeWeights node weights for ketama, a map from InetSocketAddress to
     *          weight as Integer
     * @param configuration node locator configuration
     */
    public CeilingKeyKetamaNodeLocator(List<MemcachedNode> nodes, HashAlgorithm alg,
                                       Map<InetSocketAddress, Integer> nodeWeights,
                                       KetamaNodeLocatorConfiguration configuration) {
        super();
        hashAlg = alg;
        config = configuration;
        weights = nodeWeights;
        isWeightedKetama = !weights.isEmpty();
        setKetamaNodes(nodes);
    }

    private CeilingKeyKetamaNodeLocator(ArrayBasedCeilRing nodes,
                                        HashAlgorithm alg,
                                        Map<InetSocketAddress, Integer> nodeWeights,
                                        KetamaNodeLocatorConfiguration conf) {
        super();
        ketamaNodes = nodes;
        hashAlg = alg;
        config = conf;
        weights = nodeWeights;
        isWeightedKetama = !weights.isEmpty();
    }

    public Collection<MemcachedNode> getAll() {
        return ketamaNodes.getAllNodes();
    }

    public MemcachedNode getPrimary(final String k) {
        MemcachedNode rv = getNodeForKey(hashAlg.hash(k));
        assert rv != null : "Found no node for key " + k;
        return rv;
    }

    long getMaxKey() {
        return ketamaNodes.getMaxPosition();
    }

    MemcachedNode getNodeForKey(long hash) {
        return ketamaNodes.findClosestNode(hash);
    }

    public Iterator<MemcachedNode> getSequence(String k) {
        // Seven searches gives us a 1 in 2^7 chance of hitting the
        // same dead node all of the time.
        return new ArrayBasedCeilRingIterator(k, 7, hashAlg,ketamaNodes);
    }

    public NodeLocator getReadonlyCopy() {
        return new CeilingKeyKetamaNodeLocator(ketamaNodes.roClone(), hashAlg, weights, config);
    }


    @Override
    public void updateLocator(List<MemcachedNode> nodes) {
        setKetamaNodes(nodes);
    }


    /**
     * Setup the KetamaNodeLocator with the list of nodes it should use.
     *
     * @param nodes a List of MemcachedNodes for this KetamaNodeLocator to use in
     *          its continuum
     */
    protected void setKetamaNodes(List<MemcachedNode> nodes) {
        Map<Long, MemcachedNode> newNodeMap =
                new HashMap<Long, MemcachedNode>();


        int numReps = config.getNodeRepetitions();
        int nodeCount = nodes.size();
        int totalWeight = 0;

        if (isWeightedKetama) {
            for (MemcachedNode node : nodes) {
                totalWeight += weights.get(node.getSocketAddress());
            }
        }

        for (MemcachedNode node : nodes) {
            if (isWeightedKetama) {

                int thisWeight = weights.get(node.getSocketAddress());
                float percent = (float)thisWeight / (float)totalWeight;
                int pointerPerServer = (int)((Math.floor((float)(percent * (float)config.getNodeRepetitions() / 4 * (float)nodeCount + 0.0000000001))) * 4);
                for (int i = 0; i < pointerPerServer / 4; i++) {
                    for(long position : ketamaNodePositionsAtIteration(node, i)) {
                        newNodeMap.put(position, node);
                        logger.debug("Adding node {} with weight {} in position {}", node, thisWeight, position);
                    }
                }
            } else {
                // Ketama does some special work with md5 where it reuses chunks.
                // Check to be backwards compatible, the hash algorithm does not
                // matter for Ketama, just the placement should always be done using
                // MD5
                if (hashAlg == DefaultHashAlgorithm.KETAMA_HASH) {
                    for (int i = 0; i < numReps / 4; i++) {
                        for(long position : ketamaNodePositionsAtIteration(node, i)) {
                            newNodeMap.put(position, node);
                            logger.debug("Adding node {} in position {}", node, position);
                        }
                    }
                } else {
                    for (int i = 0; i < numReps; i++) {
                        long position = hashAlg.hash(config.getKeyForNode(node, i));
                        newNodeMap.put(position, node);
                    }
                }
            }
        }

        assert newNodeMap.size() == numReps * nodes.size();

        ketamaNodes = new ArrayBasedCeilRing(newNodeMap,nodes);
    }

    private List<Long> ketamaNodePositionsAtIteration(MemcachedNode node, int iteration) {
        List<Long> positions = new ArrayList<Long>();
        byte[] digest = DefaultHashAlgorithm.computeMd5(config.getKeyForNode(node, iteration));
        for (int h = 0; h < 4; h++) {
            Long k = ((long) (digest[3 + h * 4] & 0xFF) << 24)
                    | ((long) (digest[2 + h * 4] & 0xFF) << 16)
                    | ((long) (digest[1 + h * 4] & 0xFF) << 8)
                    | (digest[h * 4] & 0xFF);
            positions.add(k);
        }
        return positions;
    }
}
