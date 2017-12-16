package org.stsffap.cep.monitoring.utils;

import java.util.Random;

/**
 * @author    Mahesh D
 */
public class GetRandomString {
    public static String get(int length) {
        final char chars[] = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
        final Random rand = new Random();
        final StringBuffer sb = new StringBuffer(length);
        while (length-- != 0) {
            sb.append(chars[rand.nextInt(chars.length)]);
        }
        return sb.toString();
    }

    public static void main(String args[]) {
        System.out.println(GetRandomString.get(24));
    }
}
