package org.sagebionetworks.repopipeline;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import affymetrix.calvin.exception.UnsignedOutOfLimitsException;
import affymetrix.fusion.cel.FusionCELData;


public class CelReader {
	
	public static CelInfo readCel(String fileName) {
        FusionCELData cel = new FusionCELData();
        cel.setFileName(fileName);
        if (cel.exists() == false) throw new IllegalArgumentException(fileName+" does not exist.");
        if (cel.read() == false) throw new RuntimeException ("Failed to read the CEL file "+fileName);
//        System.out.println("Algorithm name = " + cel.getAlg());
        String platform = cel.getChipType();
//        List<FusionTagValuePair> params = cel.getParameters();
//        for (int i=0; i<params.size(); i++)
//        {
//            FusionTagValuePair param = (FusionTagValuePair) params.get(i);
//            System.out.println(param.getTag() + " = " + param.getValue());
//        }
        
        String datHeader = null;
        try {
        	datHeader = cel.getDatHeader();
        } catch (UnsignedOutOfLimitsException e) {
        	throw new RuntimeException(e);
        }
//        System.out.println("DAT Header: "+datHeader);
        DateFormat df = new SimpleDateFormat("MM/dd/yy HH:mm:ss");
        Date scanDate = null;
        int slash = datHeader.indexOf("/");
        if (slash<0) throw new RuntimeException("No scan date.");
        int start = slash-2;
        try {
        	scanDate = df.parse(datHeader.substring(start, start+17));
        } catch (ParseException e) {
        	throw new RuntimeException(e);
        }
//        DateFormat df2 = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
//        System.out.println("Scan date: "+datHeader.substring(start, start+17)+" -> "+df2.format(date));
        return new CelInfo(scanDate, platform);
	}
}
