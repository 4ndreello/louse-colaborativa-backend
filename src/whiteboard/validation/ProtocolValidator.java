package whiteboard.validation;

public class ProtocolValidator {

    // validates if the message follows the expected basic format
    public static boolean isValid(String message) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }

        String[] parts = message.split(";");
        if (parts.length < 2) {
            return false; // must have at least type and command
        }

        String type = parts[0];
        String command = parts[1];

        switch (type) {
            case "DRAW":
                return validateDrawCommand(command, parts);
            case "ACTION":
                return validateActionCommand(command);
            default:
                return false; // unknown type
        }
    }

    private static boolean validateDrawCommand(String tool, String[] parts) {
        // base format: draw;tool;color;thickness;... (minimum 5 parts)
        if (parts.length < 5) return false;

        try {
            // attempts to validate that thickness is a number
            Integer.parseInt(parts[3]);

            return switch (tool) {
                case "PENCIL", "LINE" ->
                    // expected: X1, Y1, X2, Y2 (4 coords) -> total 8 parts
                        parts.length == 8 && areCoordinatesValid(parts, 4, 4);
                case "RECT", "OVAL", "SQUARE", "RECTANGLE", "TRIANGLE", "HEXAGON" ->
                    // expected: X, Y, W, H (4 coords) -> total 8 parts
                    // this logic works for all shapes that follow the shapetool protocol
                        parts.length == 8 && areCoordinatesValid(parts, 4, 4);
                case "TEXT" ->
                    // draw;text;color;size;x;y;content -> minimum 7 parts
                        parts.length >= 7 && areCoordinatesValid(parts, 4, 2);
                default -> false;
            };
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean validateActionCommand(String action) {
        return action.equals("CLEAR") || action.equals("UNDO");
    }

    // checks if a range of parts in the message are valid integers
    private static boolean areCoordinatesValid(String[] parts, int startIndex, int count) {
        try {
            for (int i = startIndex; i < startIndex + count; i++) {
                Integer.parseInt(parts[i]);
            }
            return true;
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            return false;
        }
    }
}