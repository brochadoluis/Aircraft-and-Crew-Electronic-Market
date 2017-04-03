package cbr;

import agents.Proposal;
import utils.Log;

import java.io.*;
import java.util.ArrayList;

/**
 * Created by Luis on 28/03/2017.
 */
public class FileManager {
    /**
     * Log
     * @since 1.0
     */
    private Log log;

    protected void setLog(Log log)
    {
        this.log = log;
    }

    /*public Data read(String dataFile) throws IOException {
        String line;
        long linenum = 0;
        Data data = null;
        ArrayList<String> header = data.getHeader();
        BufferedReader in;

        log.write("Reading datafile \"" + dataFile + "\".");
        in = new BufferedReader(new FileReader(dataFile));
        while (in.ready())
        {
            line = in.readLine();
            if (linenum == 0)
            {
                // First line in file - headings (or feature names)
                featureNames = line.split("\t");

            } else if (linenum == 1)
            {
                // Second line in file - data types
                featureTypeNames = line.split( "\t");
                try
                {
                    data = new Data(featureNames, featureTypeNames);
                } catch (ArrayIndexOutOfBoundsException e)
                {
                    throw new java.io.IOException("Error when reading file, not equal number of properties in names and types.");
                } catch (Exception e)
                {
                    throw new java.io.IOException("Error when reading file, error message:" + e.toString());
                } catch (IllegalTypeException e) {
                    e.printStackTrace();
                }
            } else	// linenum > 1
            {
                // Add the case described in this line to the dataset
                try
                {
                    data.addCase(line);
                } catch (Exception e)
                {
                    log.write("Unable to add case to set, case #" + (linenum - 1) + ", error message: " + e.toString());
                } catch (IllegalTypeException e) {
                    e.printStackTrace();
                }

            }
            linenum++;
        }
        in.close();


        // Create an empty data set if the file was empty
        if (data == null)
        {
            data = new Data();
        }


        return data;
    }*/


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
            System.out.println("heade4 seisz " + header.size());

            // First print header
            for (int i = 0; i < header.size(); i++) {
                out.print(header.get(i));
                if (i < header.size() - 1) {
                    if(i == header.size() -2){
                        out.print("\t\t");
                    }
                    else {
                        System.out.println("(header.size() - 1) " + (header.size() - 1));
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
                }

            }
        }

        out.close();
        out = null;
    }

}

