//Expression solver update to handle '4U'


if (expr instanceof NumberLiteral) {
    try {
        String numText = ((NumberLiteral) expr).getToken().getText().replaceAll("L", "");
        
        // Handle "U" suffix specifically
        if (numText.endsWith("U")) {
            numText = numText.replace("U", "");
        }

        // Parse the number after processing
        stack.push(model.intVar(Double.valueOf(numText).intValue()));
    } catch (NumberFormatException e) {
        try {
            String numText = ((NumberLiteral) expr).getToken().getText().replaceAll("L", "").replace("U", "");
            stack.push(model.intVar(Long.decode(numText).intValue()));
        } catch (NumberFormatException e1) {
            System.err.println("the given number format is not compatible with the solver!" +
                    "\n number: " + ((NumberLiteral) expr).getToken().getText());
            stack.push(model.intVar(Long.decode(((NumberLiteral) expr).getToken().getText().replaceAll("0000UL", "").replace("U", "")).intValue()));
        }
    }
}
