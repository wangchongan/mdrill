package com.alimama.quanjingmonitor.mdrillImport;

import java.io.*;
import java.util.*;
import java.nio.charset.Charset;

import org.apache.commons.logging.*;

import com.alimama.quanjingmonitor.parser.InvalidEntryException;
import com.alimama.quanjingmonitor.parser.Parser;

public class ImportReader
{
	
	
    private static final Log LOG = LogFactory.getLog(ImportReader.class.getName());

    private final static int NEW_ENTRIES_COUNT = 20;


    final Stat stat;

     RawDataReader rawDataReader;

    final Parser parser;


    static final Charset charset;
    static {
    	charset = Charset.forName("UTF-8");
    }
    
    public static abstract class RawDataReader
    {
		public  abstract void init(Map config, String confPrefix,
				     int readerIndex, int readerCount)
		    throws IOException;
	
		public abstract List<String> read()
		    throws IOException;
	
		public abstract void close()
		    throws IOException;
    }

    public ImportReader(Map conf, String confPrefix, Parser parser,
		    int readerIndex, int readerCount)
	throws IOException
    {
		stat = new Stat();
		try {
			rawDataReader=(RawDataReader) Class.forName(String.valueOf(conf.get(confPrefix+"-reader"))).newInstance();
		} catch (Throwable e1) {
			LOG.error("RawDataReader",e1);
		}
	    this.parser = parser;
    }


	public synchronized List read() throws IOException {
		List<String> rawData = rawDataReader.read();
		List entries = new ArrayList(NEW_ENTRIES_COUNT);
		if (rawData != null) {
			for (String str : rawData) {

				try {
					Object e = parser.parse(str);
					if(e!=null)
					{
						stat.valid++;
						entries.add(e);
					}else{
						stat.invalid++;
						stat.debugError(str);
					}
				} catch (InvalidEntryException iee) {
					stat.invalid++;
					stat.debugError(str);

				}
			}
		}
		return entries;
	}




    public class Stat
 {
		public long size;

		public long valid;

		public long invalid;
		public long debuglines = 0;
		long debugts = System.currentTimeMillis() / 300000;

		public Stat() {
			size = 0;
			valid = 0;
			invalid = 0;
		}

		public void debugError(String s) {
			debuglines++;

			if (debuglines < 100) {
				if (!s.isEmpty() && s.length() < 500) {
					LOG.error("######" + s);
				} else {
					if (debuglines > 0) {
						debuglines--;
					}
				}
			}

			if (debuglines % 10000 == 0) {
				long nowts = System.currentTimeMillis() / 300000;
				if (nowts != debugts) {
					debugts = nowts;
					debuglines = 0;
				}

			}

		}

		public Stat(Stat stat) {
			size = stat.size;
			valid = stat.valid;
			invalid = stat.invalid;
		}
	}
}


