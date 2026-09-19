package piotr_gorczynski.soccer2;

import java.util.Locale;

final class BangladeshPayoutAccountValidator {

    private BangladeshPayoutAccountValidator() {
    }

    static String normalize(String input) {
        String compact = input == null ? "" : input.replaceAll("[\\s()-]", "");
        if (compact.startsWith("+880")) {
            return "0" + compact.substring(4);
        }
        if (compact.startsWith("880")) {
            return "0" + compact.substring(3);
        }
        return compact;
    }

    static boolean isValid(String method, String accountNumber) {
        String normalizedMethod = method == null ? "" : method.toLowerCase(Locale.ROOT);
        if (accountNumber == null) return false;
        return switch (normalizedMethod) {
            case "bkash", "nagad" -> accountNumber.matches("01[3-9][0-9]{8}");
            case "rocket" -> accountNumber.matches("01[3-9][0-9]{9}");
            default -> accountNumber.matches("[0-9]{10,18}");
        };
    }
}
