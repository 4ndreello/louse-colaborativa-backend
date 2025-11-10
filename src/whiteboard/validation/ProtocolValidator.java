package whiteboard.validation;

public class ProtocolValidator {

    // Valida se a mensagem segue o formato básico esperado
    public static boolean isValid(String message) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }

        String[] parts = message.split(";");
        if (parts.length < 2) {
            return false; // Deve ter pelo menos TIPO e COMANDO
        }

        String type = parts[0];
        String command = parts[1];

        switch (type) {
            case "DRAW":
                return validateDrawCommand(command, parts);
            case "ACTION":
                return validateActionCommand(command);
            default:
                return false; // Tipo desconhecido
        }
    }

    private static boolean validateDrawCommand(String tool, String[] parts) {
        // Formato base: DRAW;TOOL;COLOR;THICKNESS;... (mínimo 4 partes fixas antes das coords)
        if (parts.length < 5) return false;

        try {
            // Tenta validar se espessura é número
            Integer.parseInt(parts[3]);

            switch (tool) {
                case "PENCIL":
                case "LINE":
                    // Espera: X1, Y1, X2, Y2 (4 coords) -> Total 4 + 4 = 8 partes
                    return parts.length == 8 && areCoordinatesValid(parts, 4, 4);
                case "RECT":
                case "OVAL":
                    // Espera: X, Y, W, H (4 coords) -> Total 4 + 4 = 8 partes
                    return parts.length == 8 && areCoordinatesValid(parts, 4, 4);
                case "TEXT":
                    // DRAW;TEXT;COLOR;SIZE;X;Y;CONTENT -> Mínimo 7 partes
                    return parts.length >= 7 && areCoordinatesValid(parts, 4, 2);
                default:
                    return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean validateActionCommand(String action) {
        return action.equals("CLEAR") || action.equals("UNDO");
    }

    // Verifica se um intervalo de partes da mensagem são números inteiros válidos
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