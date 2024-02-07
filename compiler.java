import java.io.FileReader;
import java.io.BufferedReader;
import java.util.HashMap;
import java.io.PrintWriter;
import java.util.ArrayList;

public class compiler {
    public static void main(String[] args) throws Exception {
        HashMap<String, String> variables = new HashMap<String, String>();
        HashMap<String, String> labels = new HashMap<String, String>();
        ArrayList<String> outputs = new ArrayList<>();
        ArrayList<String> Lines = new ArrayList<>();
        String salFile = "ex1.sal";
        String outputFileName = "ex1.bin";

        salFile = args[0];

        if (salFile.contains(".sal")){
            outputFileName=salFile.split(".sal")[0];
        }
        else{
            System.out.println("Error invalid file type");
            System.exit(0);
        }

        int count = 0;
        BufferedReader reader = new BufferedReader(new FileReader(salFile));

        char currentSection = ' ';
        String theLine;
        while ((theLine = reader.readLine()) != null) {
            theLine = theLine.trim();
            Lines.add(theLine);
        }
        for (String line : Lines) {
            if (line.equals(".data")) {
                currentSection = 'd';
                count = 0;
            } else if (line.equals(".code")) {
                currentSection = 'c';
                count = 1;
            }

            if (currentSection == 'd' & !line.equals(".data")) {
                dataSection(line, count, variables);
                count++;
            }
            if (currentSection == 'c' & !line.equals(".code")) {
                count = codeSection(line, count, variables, labels, outputs, Lines);
            }

        }
        reader.close();
        System.out.println(variables);
        System.out.println(labels);
        System.out.println(outputs);
        printValues(outputs, outputFileName);
    }

    // creates opcode along side the variable in a dict. cant go past 16-bits!!
    private static void dataSection(String line, int count, HashMap<String, String> variables) {
        if (count != 0) {
            variables.put(line, DecimalToBinaryCalc(count, 6));
        } else
            variables.put(line, "000000");
    }

    // decimal to binary, converts counter to a binary version
    private static String DecimalToBinaryCalc(int DecimalNumber, int numberOfZeros) {
        String result = "";
        while (DecimalNumber != 0) {
            result += DecimalNumber % 2;
            DecimalNumber = DecimalNumber / 2;
        }
        int resultLength = result.length();
        for (int i = 0; i < (numberOfZeros - resultLength); i++) {
            result += "0";
        }
        result = new StringBuilder(result).reverse().toString();
        return result;
    }

    private static int codeSection(String line, int count, HashMap<String, String> variables,
            HashMap<String, String> labels, ArrayList<String> outputs, ArrayList<String> Lines) {
        String theString = line.split("//")[0];
        String output = "";
        HashMap<String, String> Instructions = new HashMap<String, String>();
        Instructions.put("ADD", "0000");
        Instructions.put("SUB", "0001");
        Instructions.put("AND", "0010");
        Instructions.put("OR", "0011");
        Instructions.put("JMP", "0100000");
        Instructions.put("JGT", "0101");
        Instructions.put("JLT", "0110");
        Instructions.put("JEQ", "0111");
        Instructions.put("INC", "1001");
        Instructions.put("DEC", "1010");
        Instructions.put("NOT", "1011");
        Instructions.put("LOAD", "1100");
        Instructions.put("STORE", "1101");

        // calc loop
        if (theString.contains(":")) {
            theString.replaceAll("\\s+", "");
            labels.put(theString.substring(0, theString.length() - 1), DecimalToBinaryCalc(count - 1, 6));
            return count;
        } else
            count++;

        // calc instruction
        String instruction = "";
        for (int i = 0; i < theString.length() - 1; i++)
            if (theString.substring(i, i + 1).equals(" ")) {
                instruction = theString.substring(0, i);
                theString = theString.substring(i + 1);
                output += Instructions.get(instruction);
                break;
            }

        // calc register
        int registerNum = 0;
        if (!instruction.equals("JMP"))
            if (theString.toUpperCase().contains("R")) {
                for (int i = theString.toUpperCase().indexOf("R"); i < theString.length(); i++) {
                    if (theString.substring(i, i + 1).equals(" ") || theString.substring(i, i + 1).equals(",")
                            || i == theString.length() - 1) {
                        if (i == theString.length() - 1) {
                            registerNum = Integer.parseInt(theString.substring(1, i + 1));
                            theString = "";
                            output += DecimalToBinaryCalc(registerNum, 3);
                            break;
                        } else {
                            registerNum = Integer.parseInt(theString.substring(1, i));
                            theString = theString.substring(i + 1);
                            output += DecimalToBinaryCalc(registerNum, 3);
                            break;
                        }
                    }
                }
            } else {
                System.out.println("Error invalid instruction");
                System.exit(0);
            }

        // clac RVA
        String RVA = "";
        if (theString.contains("#")) {
            theString = theString.split("#")[1];
            RVA = "01";
        } else if (theString.toLowerCase().contains("r")) {
            theString = theString.toLowerCase().split("r")[1];
            RVA = "00";
        } else if (theString.equals("") || theString == null) {
            RVA = "11";
        } else if (theString.toLowerCase().contains("r")) {
            theString = theString.toLowerCase().split("r")[1];
            RVA = "00";
        } else
            RVA = "10";

        String size = "0";
        theString = theString.replaceAll("\\s+", "");

        // calc opcode
        if (variables.get(theString) != null)
            output += size + RVA + variables.get(theString);
        else if (labels.get(theString) != null)
            output += size + RVA + labels.get(theString);
        else if (checkAlphabet(theString) == true & theString != "") {
            unfoundLable(count, variables, labels, Lines);
            if (labels.get(theString) != null)
                output += size + RVA + labels.get(theString);
        } else if (instruction.equals("INC") || instruction.equals("DEC") | instruction.equals("NOT"))
            output += size + RVA + "000000";
        else if (Integer.parseInt(theString) > 64) {
            size = "1";
            output += size + RVA + "000000";
            outputs.add(output);
            output = DecimalToBinaryCalc(Integer.parseInt(theString), 16);
        } else
            output += size + RVA + DecimalToBinaryCalc(Integer.parseInt(theString), 6);

        outputs.add(output);
        return count;
    }

    private static void printValues(ArrayList<String> outputs, String outputFileName) {
        try {
            PrintWriter writer = new PrintWriter(outputFileName + ".bin", "UTF-8");
            for (int i = 0; i < outputs.size(); i++)
                writer.println(outputs.get(i) + " ");
            writer.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    } // end main loop

    private static void unfoundLable(int count, HashMap<String, String> variables,
            HashMap<String, String> labels, ArrayList<String> Lines) {
        String[] tmpArr;
        int extraLines = 0;
        String tmpStr = "";
        for (int i = count + 1; i < Lines.size(); i++)
            if (!Lines.get(i).contains(":")) {
                tmpArr = Lines.get(i).split(" ");
                tmpStr = tmpArr[tmpArr.length - 1];
                if (!Lines.get(i).contains("#"))
                    if (!Lines.get(i).toLowerCase().contains("r"))
                        if (!variables.containsKey(tmpStr))
                            if (Integer.parseInt(tmpStr) > 64)
                                extraLines++;
            } else {
                tmpStr = Lines.get(i).replaceAll("\\s+", "");
                labels.put(tmpStr.substring(0, tmpStr.length() - 1), DecimalToBinaryCalc(i -1 + extraLines, 6));
            }
    }

    private static boolean checkAlphabet(String word) {
        for (char c : word.toCharArray()) {
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'))) {
                return false; // if not in alphabet
            }
        }
        return true; // if is in alphabet
    }
}