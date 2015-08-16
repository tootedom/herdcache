package org.greencheek.caching.herdcache.util;

import java.util.Arrays;

/**
 *
 */
public class CeilIndexFind {

    /**
     * Find the index in the array at which the first value greater than or equal
     * to the given index is.   If index is greater than the maximum value in the
     * array then 0 is returned.  If index is less than or equal to the first element in the
     * array then 0 is returned.
     *
     * Uses a binary search type algorithm ( O(log n) ) to find the closest largest value in the array.
     *
     * @param keys The sorted list of values
     * @param index The value to find the closest item
     * @return
     */
    public static long findCeilIndex(long[] keys, long index) {
//        if (keys.length == 0) {
//            throw new IllegalArgumentException("array cannot be empty");
//        }

        int low = 0;
        int high = keys.length - 1;

        // Check for edge cases
        if (index > keys[high]) return 0;
        if (index <= keys[low]) return 0;

//        int found = 0;
        while(true) {
//            if (low > high) {
//                return 0;
//            }
            // div by two
            int mid = (low + high) >>> 1;
            long midVal = keys[mid];
            if (midVal == index) return mid;
            else if (midVal < index) {
                low = mid + 1;
                if (keys[low] >= index) return low;
            }
            else if (midVal > index) {
//                found = mid;
                high = mid - 1;
                if (keys[high] < index) return mid;
            }

//
        }

        // if here return the first in the ring (first)
//        if (found == -1) {
//            return 0;
//        }
//        return 0;
    }

    public static void main(String[] args) {
        long arr[] = {1, 2, 8, 10, 10, 12, 19, 21, 66, 67, 68, 69, 77, 101};
        System.out.println(findCeilIndex(arr, -1));  // 0
        System.out.println(findCeilIndex(arr,20));   // 7
        System.out.println(findCeilIndex(arr,18));   // 6
        System.out.println(findCeilIndex(arr,9));    // 3
        System.out.println(findCeilIndex(arr,3));    // 2
        System.out.println(findCeilIndex(arr,8));    // 2
        System.out.println(findCeilIndex(arr,1));    // 0
        System.out.println(findCeilIndex(arr,0));    // 0
        System.out.println(findCeilIndex(arr,19));   // 6

        System.out.println(findCeilIndex(arr,11));   // 5
        System.out.println(findCeilIndex(arr,2));   // 1
        System.out.println(findCeilIndex(arr,13));   // 6
        System.out.println(findCeilIndex(arr,Integer.MAX_VALUE));   // 0
        System.out.println(findCeilIndex(arr,Integer.MIN_VALUE));   // 0
        System.out.println(findCeilIndex(arr,1));   // 0

        System.out.flush();
        try {
//            System.out.println(findCeilIndex(new long[0], Integer.MIN_VALUE));   // 0
        } catch(IllegalArgumentException e) {
            e.printStackTrace(System.out);
        }

        arr = new long[10000];
        for(int i = 0;i<10000;i++) {
            arr[i] = i*2;
        }

        System.out.println(Arrays.toString(arr));
        System.out.println(findCeilIndex(arr,19998));   // 9999
        System.out.println(findCeilIndex(arr,19999));   // 0
        System.out.println(findCeilIndex(arr,20000));   // 0
        System.out.println(findCeilIndex(arr,2000));   // 1000
        System.out.println(findCeilIndex(arr,999));   // 500
        System.out.println(findCeilIndex(arr,1000));   // 500

        arr = new long[10000];
        int j = -10000;
        for(int i = 0;i<10000;i++) {
            arr[i] = j + (i * 2);

        }

        System.out.println(Arrays.toString(arr));
        System.out.println(findCeilIndex(arr,-10000)); //0
        System.out.println(findCeilIndex(arr,-11000)); //0
        System.out.println(findCeilIndex(arr,-9999)); //1
        System.out.println(findCeilIndex(arr,-9998)); //1
        System.out.println(findCeilIndex(arr,-9993)); //4
        System.out.println(findCeilIndex(arr,9998));   // 9999
        System.out.println(findCeilIndex(arr,14999));   // 0
        System.out.println(findCeilIndex(arr,14996));   // 0
        System.out.println(findCeilIndex(arr,2000));   // 6000
        System.out.println(findCeilIndex(arr,0));   // 5000
        System.out.println(findCeilIndex(arr,1));   // 5001
        System.out.println(findCeilIndex(arr,-2));   // 4999
        System.out.println(findCeilIndex(arr,9));   // 5005

        arr = new long[1];

        arr[0] = 10;

        System.out.println(findCeilIndex(arr,9));   // 0
        System.out.println(findCeilIndex(arr,11));   // 0
        System.out.println(findCeilIndex(arr,Integer.MAX_VALUE));   // 0


        arr = new long[] { -5, -3, -1};

        System.out.println(findCeilIndex(arr,9));   // 0
        System.out.println(findCeilIndex(arr,11));   // 0
        System.out.println(findCeilIndex(arr,Integer.MAX_VALUE));   // 0

        System.out.println(findCeilIndex(arr,-10));   // 0
        System.out.println(findCeilIndex(arr,-4));   // 1
        System.out.println(findCeilIndex(arr,-2));   // 2

    }
}
