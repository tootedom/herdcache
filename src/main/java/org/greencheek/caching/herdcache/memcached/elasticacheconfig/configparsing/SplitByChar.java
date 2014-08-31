package org.greencheek.caching.herdcache.memcached.elasticacheconfig.configparsing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by dominictootell on 30/08/2014.
 */
public class SplitByChar {

    public static List<String> split(String str,char separatorChar) {
        return split(str,separatorChar,true);
    }

    public static List<String> split(String str,char separatorChar,boolean compact) {
        if (str == null) {
            return Collections.EMPTY_LIST;
        }
        int len = str.length();

        if (len == 0) {
            return Collections.EMPTY_LIST;
        }

        List<String> list = new ArrayList<String>();
        int i = 0;
        int start = 0;

        boolean isMatch = compact ? false : true;
        boolean lastMatch = false;

        while (i < len) {
            if (str.charAt(i) == separatorChar) {
                if (isMatch) {
                    String s = str.substring(start, i);
                    if(compact) {
                        if(s.length()!=0) {
                            list.add(s);
                        }
                    }
                    else {
                        list.add(s);
                    }
                    isMatch = compact ? false : true;
                    lastMatch = true;
                }
                i+=1;
                start = i;
            }
            else {
                lastMatch = false;
                isMatch = true;
                i += 1;
            }
        }

        if (isMatch || lastMatch) {
            String s = str.substring(start, i);
            if(compact) {
                if(s.length()!=0) {
                    list.add(s);
                }
            }
            else {
                list.add(s);
            }

        }

        return list;
    }
}
