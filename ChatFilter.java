import java.io.*;

public class ChatFilter {
    private File file;

    public ChatFilter(String badWordsFileName) {
        this.file = new File(badWordsFileName);
    }

    public File getFile() {
        return this.file;
    }

    public String filter(String msg) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(getFile()));
            String line = bufferedReader.readLine();
            while (line != null) {
                String replace = "";
                for (int i = 0; i < line.length(); i++) {
                    replace += "*";
                }
                msg = msg.replaceAll("(?i)" + line, replace);
                line = bufferedReader.readLine();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return msg;
    }
}
