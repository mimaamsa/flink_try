package org.stsffap.cep.monitoring.utils;

import java.util.Random;

/**
 * @author    Mahesh D
 */
public class GetRandomIpString {
    public static String get() {
        final Integer bound = 256;
        final Random rand = new Random();
        return String.format("%d.%d.%d.%d",
                rand.nextInt(bound),
                rand.nextInt(bound),
                rand.nextInt(bound),
                rand.nextInt(bound));
    }

    public static void main(String args[]) {
        System.out.println(GetRandomIpString.get());
    }
}
