package utils;

import java.io.*;
import java.util.*;

/**
 * Created by Luis on 28/03/2017.
 */
public class Log {

    /**
     * Is the logger ready to log?
     */
    private boolean ready;

    /**
     * Name of the file to use as log file
     */
    private String logFile;

    /**
     * Output object
     * @since 1.0
     */
    private transient PrintWriter out;

    /**
     * Calendar object
     * @since 1.0
     */
    private GregorianCalendar calendar;

    /**
     * Will the logger output data to screen? If <code>true</code> then not.
     * @since 1.0
     */
    private boolean silent;


    /**
     * Empty constructor
     *
     * @since 1.0
     */
	/* History: Date		Name	Explanation (possibly multi row)
	*/
    public Log()
    {
        ready = false;
        logFile = "";
        calendar = new java.util.GregorianCalendar();
        silent = false;
    }
    /**
     * Constructor that initiates the logger
     *
     * @param logFile path to the file to write log to. May be set to "null"
     *		which means no logging.
     * @since 1.0
     */
	/* History: Date		Name	Explanation (possibly multi row)
	*/
    public Log(String logFile)
    {
        ready = false;
        this.logFile = logFile;

        try
        {
            calendar = new java.util.GregorianCalendar();
            if (logFile != null)
            {
                out = new PrintWriter(new BufferedWriter(new FileWriter(logFile)));
                ready = true;
            }
        } catch (Exception e)
        {
            this.logFile = "";
        }
    }


    /**
     * Returns the current state
     *
     * @return the current ready state (true/false)
     * @since 1.0
     */
	/* History: Date		Name	Explanation (possibly multi row)
	*/
    public boolean getReady()
    {
        return this.ready;
    }


    /**
     * Returns the current silence state
     *
     * @return the current silence state
     * @since 1.0
     */
	/* History: Date		Name	Explanation (possibly multi row)
	*/
    public boolean getSilent()
    {
        return this.silent;
    }

    /**
     * Sets the current silence state
     *
     * @param silent the silence state to set
     * @since 1.0
     */
	/* History: Date		Name	Explanation (possibly multi row)
	*/
    public void setSilent(boolean silent)
    {
        this.silent = silent;
    }


    /**
     * Returns the current log file
     *
     * @return the current log file
     * @since 1.0
     */
	/* History: Date		Name	Explanation (possibly multi row)
	*/
    public String getLogFile()
    {
        return this.logFile;
    }

    /**
     * Sets the current log file
     *
     * @param logFile the file to use as logFile
     * @since 1.0
     */
	/* History: Date		Name	Explanation (possibly multi row)
	*/
    public void setLogFile(String logFile)
    {
        this.logFile = logFile;
    }


    /**
     * Reads the object from stream
     *
     * @param in stream to read from
     * @since 1.0
     */
	/* History: Date		Name	Explanation (possibly multi row)
	*/
    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        try
        {
            if (logFile != null)
            {
                out = new PrintWriter(new BufferedWriter(new FileWriter(logFile)));
            }
        } catch (Exception e)
        {
            ready = false;
            this.logFile = "";
        }
    }


    /**
     * Writes a message to log file, always starts with current date and
     *		time and ends with a newline
     *
     * @param message the message to write
     */
    public void write(String message)
    {
        String logMessage = new String(getTime() + " " + message);

        if (!silent)
        {
            System.err.print(logMessage);
        }
        if (ready)
        {
            out.write(logMessage);
        }
        writeNL();
    }


    /**
     * Writes a message to log file. No date, time or newline.
     *
     * @param message the message to write
     */
    public void writeShort(String message)
    {
        if (!silent)
        {
            System.err.print(message);
        }
        if (ready)
        {
            out.write(message);
            out.flush();
        }
    }


    /**
     * Writes only a newline to log file.
     *
     */
    public void writeNL()
    {
        if (!silent)
        {
            System.err.println();
        }
        if (ready)
        {
            out.println();
            out.flush();
        }
    }


    /**
     * Returns current time in the format YYYY-MM-DD HH:MM:SS.ms
     *
     * @return current time
     */
    private String getTime()
    {
        int year = calendar.get(Calendar.YEAR);
        short month = (short) (calendar.get(Calendar.MONTH) + 1);
        short day = (short) calendar.get(Calendar.DAY_OF_MONTH);
        short hour = (short) calendar.get(Calendar.HOUR);
        short min = (short) calendar.get(Calendar.MINUTE);
        short sec = (short) calendar.get(Calendar.SECOND);
        short ms = (short) calendar.get(Calendar.MILLISECOND);
        StringBuffer sb;

        sb = new StringBuffer().append(year).append("-");
        if (month < 10)
            sb.append("0");
        sb.append(month).append("-");
        if (day < 10)
            sb.append("0");
        sb.append(day).append(" ").append(hour).append(":");
        if (min < 10)
            sb.append("0");
        sb.append(min).append(":");
        if (sec < 10)
            sb.append("0");
        sb.append(sec).append(".");
        if (ms < 100)
            sb.append("0");
        if (ms < 10)
            sb.append("0");
        sb.append(ms);

        return sb.toString();
    }
}
