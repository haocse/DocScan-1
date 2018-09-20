package at.ac.tuwien.caa.docscan.ui.document;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Date;

/**
 * Created by fabian on 28.11.2017.
 */

public class DocumentMetaData {

    public static final int NO_RELATED_UPLOAD_ID_ASSIGNED = -1;

//    Strings needed to parse QR codes:
    private static final String QR_RELATED_UPLOAD_ID =  "relatedUploadId";
    private static final String QR_AUTHORITY =          "authority";
    private static final String QR_TITLE =              "title";
    private static final String QR_IDENTIFIER =         "identifier";
    private static final String QR_TYPE =               "type";
    private static final String QR_TYPE_HIERARCHY =     "hierarchy description";
    private static final String QR_TYPE_URI =           "uri";
    private static final String QR_SIGNATURE =          "callNumber";
    private static final String QR_DESCRIPTION =        "description";
    private static final String QR_DATE =               "date";

    private String mTitle;
    private String mAuthority;
    private String mHierarchy;
    private String mSignature;
    private String mUri;

    private String mDescription;
    private Date mDate;
    private int mRelatedUploadId = NO_RELATED_UPLOAD_ID_ASSIGNED;

    public String getTitle() { return mTitle; }
    public String getDescription() { return mDescription; }
    public String getAuthority() { return mAuthority; }
    public String getHierarchy() { return mHierarchy; }
    public String getUri() { return mUri; }
    public String getSignature() { return mSignature; }
    public Date getDate() { return mDate; }
    public int getRelatedUploadId() { return mRelatedUploadId; }

    private DocumentMetaData() {

    }

    public String toJSON() {

        Gson gson = new GsonBuilder().setFieldNamingPolicy(
                FieldNamingPolicy.UPPER_CAMEL_CASE).create();
        String json = gson.toJson(this);

        return json;

    }

    private static class IdentifierValue {

        private String mIdentifier, mValue;

        private IdentifierValue(String identifier, String value) {
            mIdentifier = identifier;
            mValue = value;
        }

        private String getIdentifier() { return mIdentifier; }
        private String getValue() { return mValue; }
    }

    /**
     * Escapes characters that are not allowed for XML string. Replaces the characters with entity
     * references.
     * @param text
     * @return
     */
    private static String escapeReferences(String text) {

        String result = text;
        result = result.replace("&", "&amp;");
//        result = result.replace("<", "&lt");
//        result = result.replace(">", "&gt");
//        result = result.replace("\"", "&quot;");
//        result = result.replace("'", "&apos");

        return result;

    }

    public static DocumentMetaData parseXML(String text) {

        try {

            XmlPullParserFactory xmlFactoryObject = XmlPullParserFactory.newInstance();
            XmlPullParser parser = xmlFactoryObject.newPullParser();

            String escapedText = escapeReferences(text);

            InputStream stream = new ByteArrayInputStream(escapedText.getBytes(Charset.forName("UTF-8")));
            parser.setInput(stream, null);

            DocumentMetaData documentMetaData = new DocumentMetaData();

            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                String name = parser.getName();

                if (name.equals(QR_AUTHORITY))
                    documentMetaData.mAuthority = readTextFromTag(parser, QR_AUTHORITY);

                else if (name.equals(QR_IDENTIFIER)) {
                    IdentifierValue iv = readIdentifier(parser);
                    if (iv.getIdentifier().equals(QR_TYPE_HIERARCHY))
                        documentMetaData.mHierarchy = iv.getValue();
                    else if (iv.getIdentifier().equals(QR_TYPE_URI))
                        documentMetaData.mUri = iv.getValue();
                }
                else if (name.equals(QR_TITLE))
                    documentMetaData.mTitle = readTextFromTag(parser, QR_TITLE);
                else if (name.equals(QR_DESCRIPTION))
                    documentMetaData.mDescription = readTextFromTag(parser, QR_DESCRIPTION);
                else if (name.equals(QR_SIGNATURE))
                    documentMetaData.mSignature = readTextFromTag(parser, QR_SIGNATURE);
                else if (name.equals(QR_RELATED_UPLOAD_ID)) {
                    String rId = readTextFromTag(parser, QR_RELATED_UPLOAD_ID);
                    documentMetaData.mRelatedUploadId = Integer.valueOf(rId);
                }
                else if (name.equals(QR_DATE))
                    skip(parser);

            }

            return documentMetaData;


        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        return null;

    }



    private static void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

    private static String readTextFromTag(XmlPullParser parser, String tag) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, null, tag);
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, null, tag);
        return title;
    }


    private static IdentifierValue readIdentifier(XmlPullParser parser) throws IOException, XmlPullParserException {

        parser.require(XmlPullParser.START_TAG, null, QR_IDENTIFIER);
        String tag = parser.getName();
        String identifierType = parser.getAttributeValue(null, QR_TYPE);
        String value = null;

        if (tag.equals(QR_IDENTIFIER)) {
            if (identifierType.equals(QR_TYPE_HIERARCHY) || identifierType.equals(QR_TYPE_URI))
                value = readText(parser);
        }

        parser.require(XmlPullParser.END_TAG, null, QR_IDENTIFIER);

        return new IdentifierValue(identifierType, value);

    }


    private static String readText(XmlPullParser parser) throws IOException, XmlPullParserException {

        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;

    }






}
