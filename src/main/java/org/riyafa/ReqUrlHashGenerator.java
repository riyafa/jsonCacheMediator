package org.riyafa;

import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ReqUrlHashGenerator implements DigestGenerator{

    private static final Log log = LogFactory.getLog(ReqUrlHashGenerator.class);

    public static final String MD5_DIGEST_ALGORITHM = "MD5";

    public String getDigest(MessageContext msgContext) throws CachingException {

        if (msgContext.getTo() == null) {
            return null;
        }

        String toAddress = msgContext.getTo().getAddress();
        byte[] digest = getDigest(toAddress, MD5_DIGEST_ALGORITHM);
        return digest != null ? getStringRepresentation(digest) : null;
    }

    public byte[] getDigest(String toAddress, String digestAlgorithm) throws CachingException {

        byte[] digest = new byte[0];
        try {
            MessageDigest md = MessageDigest.getInstance(digestAlgorithm);
            md.update(toAddress.getBytes("UnicodeBigUnmarked"));
            digest = md.digest();
        } catch (NoSuchAlgorithmException e) {
            handleException("Can not locate the algorithm " +
                    "provided for the digest generation : " + digestAlgorithm, e);
        } catch (UnsupportedEncodingException e) {
            handleException("Error in generating the digest " +
                    "using the provided encoding : UnicodeBigUnmarked", e);
        }
        
        return digest;
    }

    private void handleException(String message, Throwable cause) throws CachingException {
        log.debug(message, cause);
        throw new CachingException(message, cause);
    }

    public String getStringRepresentation(byte[] array) {

        StringBuffer strBuff = new StringBuffer(array.length);
        for (int i = 0; i < array.length; i++) {
            strBuff.append(array[i]);
        }
        return strBuff.toString();
    }
}
