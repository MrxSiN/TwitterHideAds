package my.MrxSiN.twitterhideads;

import java.util.Random;

/** Randomized inputs shared by the parity tests. Seeds are fixed, so runs repeat. */
final class ParityInputs {
    private static final String[] WORDS = {
            "conversationthread", "search", "conversation", "tweetdetailrelatedtweets", "x",
            "promoted", "tweet", "trend", "abc", "promote", "tweets", "cursor", "top", "who", "to", "follow"
    };
    private static final String[] TRAILERS = {"abc", "6707f8b5db57b19e", "1", "a_b", "_", "deadbeef", "Z9"};
    private static final String NOISE = "-_ .#aZ0\t\néKİſｐ\u0000😀p1";

    private ParityInputs() {
    }

    static String digits(Random random) {
        StringBuilder out = new StringBuilder();
        int length = 1 + random.nextInt(19);
        for (int index = 0; index < length; index++) {
            out.append((char) ('0' + random.nextInt(10)));
        }
        return out.toString();
    }

    /** Near-valid entry identifiers with occasional mutations. */
    static String entry(Random random) {
        StringBuilder out = new StringBuilder();
        int modules = random.nextInt(3);
        for (int m = 0; m < modules; m++) {
            int words = 1 + random.nextInt(2);
            for (int w = 0; w < words; w++) {
                out.append(WORDS[random.nextInt(WORDS.length)]).append('-');
            }
            out.append(digits(random)).append('-');
        }
        if (random.nextBoolean()) {
            out.append("promoted-");
            int kinds = 1 + random.nextInt(2);
            for (int k = 0; k < kinds; k++) {
                out.append(random.nextBoolean() ? "tweet" : WORDS[random.nextInt(WORDS.length)]).append('-');
            }
        } else {
            out.append("tweet-");
        }
        out.append(digits(random));
        int trailers = random.nextInt(4);
        for (int t = 0; t < trailers; t++) {
            out.append('-').append(TRAILERS[random.nextInt(TRAILERS.length)]);
        }
        String value = out.toString();
        if (random.nextInt(5) == 0) {
            value = value.toUpperCase(java.util.Locale.ROOT);
        }
        return mutate(random, value);
    }

    static String mutate(Random random, String value) {
        if (random.nextInt(10) < 4) {
            StringBuilder mutated = new StringBuilder(value);
            int position = random.nextInt(mutated.length() + 1);
            char c = NOISE.charAt(random.nextInt(NOISE.length()));
            switch (random.nextInt(3)) {
                case 0:
                    mutated.insert(position, c);
                    break;
                case 1:
                    if (position < mutated.length()) {
                        mutated.setCharAt(position, c);
                    }
                    break;
                default:
                    if (position < mutated.length()) {
                        mutated.deleteCharAt(position);
                    }
                    break;
            }
            value = mutated.toString();
        }
        return value;
    }

    /** Any text a model field might hold. */
    static String text(Random random) {
        switch (random.nextInt(8)) {
            case 0:
            case 1:
            case 2:
                return entry(random);
            case 3: {
                StringBuilder out = new StringBuilder();
                int length = random.nextInt(14);
                for (int index = 0; index < length; index++) {
                    out.append("promtedwPROMTEDW-_0123 aZKİ".charAt(random.nextInt(27)));
                }
                return out.toString();
            }
            case 4:
                return new String[]{"", "I was promoted today", "Promoted by nobody", "hello world",
                        "not-promoted-x", "unpromoted-1", "promoted-deal", "PROMOTED"}[random.nextInt(8)];
            case 5: {
                // Long values cross the 256 limit and several 255-code chunks.
                StringBuilder out = new StringBuilder();
                int repeats = 20 + random.nextInt(60);
                String unit = entry(random);
                for (int index = 0; index < repeats; index++) {
                    out.append(unit);
                }
                return out.toString();
            }
            case 6: {
                StringBuilder out = new StringBuilder();
                int length = random.nextInt(8);
                for (int index = 0; index < length; index++) {
                    out.append((char) random.nextInt(0x10000));
                }
                return out.toString();
            }
            default:
                return mutate(random, "promoted-tweet-" + digits(random));
        }
    }
}
