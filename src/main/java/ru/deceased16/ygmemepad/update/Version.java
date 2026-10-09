package ru.deceased16.ygmemepad.update;

import java.util.ArrayList;
import java.util.List;

public final class Version {

    private Version() {
    }

    public static int compare(String a, String b) {
        List<Integer> x = parse(a);
        List<Integer> y = parse(b);
        int n = Math.max(x.size(), y.size());
        for (int i = 0; i < n; i++) {
            int p = i < x.size() ? x.get(i) : 0;
            int q = i < y.size() ? y.get(i) : 0;
            if (p != q) {
                return Integer.compare(p, q);
            }
        }
        return 0;
    }

    static List<Integer> parse(String version) {
        List<Integer> parts = new ArrayList<>();
        if (version == null) {
            return parts;
        }
        String s = version.trim();
        if (s.startsWith("v") || s.startsWith("V")) {
            s = s.substring(1);
        }

        StringBuilder number = new StringBuilder();
        for (int i = 0; i <= s.length(); i++) {
            char c = i < s.length() ? s.charAt(i) : '.';
            if (Character.isDigit(c)) {
                number.append(c);
                continue;
            }
            if (number.length() > 0) {
                try {
                    parts.add(Integer.parseInt(number.toString()));
                } catch (NumberFormatException e) {
                    parts.add(Integer.MAX_VALUE);
                }
                number.setLength(0);
            }
            if (c != '.') {
                break;
            }
        }
        return parts;
    }
}
