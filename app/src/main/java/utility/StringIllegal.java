package utility;

import android.widget.EditText;

public class StringIllegal {

    public static final String[] illegalChar = {"*", "|", "\\", "/", "\"", ":", "<", ">", "?", "%", "{", "}", "^"};


    /**
     * Check if the string each char is illegal
     *
     * @param //input editText;
     * @return true = illegal char;
     */
    public static boolean checkIllegalChar(EditText editText) {
        String s = editText.getText().toString();
        if (s.length() == 0) return false;

        for (int i = 0; i < s.length(); i++) {
            for (int k = 0; k < illegalChar.length; k++) {
                if (s.substring(i, i + 1).equals(illegalChar[k])) {
                    editText.setText(s.replace(s.substring(i, i + 1), ""));
                    editText.setSelection(i);
                    return true;
                }
            }
        }
        return false;
    }

    public static void checkFirstSpaceChar(EditText editText) {
        String s = editText.getText().toString();

        if (s.length() == 0) return;

        if (s.substring(0, 1).equals(" ")) {
            editText.setText(s.replaceFirst(s.substring(0, 1), ""));
            editText.setSelection(0);

            checkFirstSpaceChar(editText);
        }
    }
}
