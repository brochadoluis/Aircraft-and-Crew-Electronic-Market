package cbr;

import utils.Log;

import java.io.*;
import java.util.ArrayList;

/**
 * Created by Luis on 28/03/2017.
 */
public class FileManager {


    public Data read(String dataFile) throws IOException {
        String line;
        long lineNum = 0;
        Data data = new Data();
        String[] headerList;
        String[] unprocessedLine;
        BufferedReader in;

//        log.write("Reading datafile \"" + dataFile + "\".");
        in = new BufferedReader(new FileReader(dataFile));
        while (in.ready())
        {
            ArrayList<String> processedLine = new ArrayList<>();
            line = in.readLine();
            if(lineNum > 0 ){
                //Splits line by tabs, creates a case (excludes tabs from the String[] obtained)
                //Add the case described in this line to the dataset
                unprocessedLine = line.split( "\t");
                for (String s : unprocessedLine) {
                    if(!s.equals("   ") && !s.isEmpty()){
                        processedLine.add(s);
                    }
                }
                data.addCase(processedLine);
            }
            lineNum++;
        }
        in.close();

        // Create an empty data set if the file was empty
        if (data == null)
        {
            data = new Data();
        }
        return data;
    }


    /**
     * Saves the data to a file
     *
     * @param data the data to save
     * @param fileName the name of the file in which to store the data.
     *		If null then store in current file
     * @return nothing
     * @throws java.io.IOException
     * @since 1.0
     */
	/* History: Date		Name	Explanation (possibly multi row)
	*/
    public void save(Data data , String fileName)
            throws java.io.IOException
    {
        ArrayList<String> header = data.getHeader();
        PrintWriter out = null;

        if (data == null)
        {
//            log.write("No data to save");
            return;
        }

        if (fileName != null) {
//            log.write("Saving to datafile \"" + fileName + "\".");

            out = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));
            // First print header
            for (int i = 0; i < header.size(); i++) {
                out.print(header.get(i));
                if (i < header.size() - 1) {
                    if(i == header.size() -2){
                        out.print("\t\t");
                    }
                    else {
                        out.print("\t");
                    }
                }
                else {
                    out.println();
                }
            }
            // Then print cases, actions and outcomes
            for (int i = 1; i < data.getSize(); i++) {
                for(int j = 0; j < data.getLineSize(); j++){
                    out.print("\t");
                    if(data.getString(i,j) != null) {
                        out.print(data.getString(i, j));
                        if (j < data.getLineSize() - 1) {
                            switch (j) {
                                case 0:
                                    out.print("\t\t\t");
                                    break;
                                case 1:
                                case 3:
                                case 4:
                                    out.print("\t\t\t\t");
                                    break;
                                case 2:
                                    out.print("\t\t\t   ");
                                    break;
                                default:
                                    break;
                            }
                        } else {
                            out.println();
                        }
                    }
                    else
                        out.print("null");
                }
            }
        }

        out.close();
        out = null;
    }

}

