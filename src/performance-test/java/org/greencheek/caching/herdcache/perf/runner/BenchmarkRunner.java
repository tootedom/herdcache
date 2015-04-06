package org.greencheek.caching.herdcache.perf.runner;

import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;

import java.util.concurrent.TimeUnit;

/**
 * Created by dominictootell on 03/04/2015.
 */
public class BenchmarkRunner {
    public static void main(String[] args) throws RunnerException {

        Options opt = new OptionsBuilder()
                .include("org.greencheek.caching.herdcache.perf.benchmarks.*")
                .warmupIterations(20)
                .measurementIterations(20)
                .timeUnit(TimeUnit.MILLISECONDS)
                .jvmArgs(JvmArgs.getJvmArgs())
                .forks(2)
                .threads(4)
                .resultFormat(ResultFormatType.TEXT)
                .verbosity(VerboseMode.EXTRA)
                .build();

        new Runner(opt).run();
    }

}
