package android.netinf.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import android.os.Environment;


public class Ndo implements Serializable {

    public static class Builder {

        private String mAuthority = "";
        private String mAlgorithm;
        private String mHash;
        private Set<Locator> mLocators = new HashSet<Locator>();
        private Metadata mMetadata = new Metadata();
        private long mTimestamp = System.currentTimeMillis();

        public Builder(Ndo ndo) {
            mAuthority = ndo.mAuthority;
            mAlgorithm = ndo.mAlgorithm;
            mHash = ndo.mHash;
            mLocators.addAll(ndo.mLocators);
            mMetadata = new Metadata(ndo.mMetadata);
            mTimestamp = ndo.mTimestamp;
        }

        public Builder(String algorithm, String hash) {
            mAlgorithm = algorithm;
            mHash = hash;
        }

        public Builder authority(String authority) { mAuthority = authority; return this; }
        public Builder locator(Locator locator) { mLocators.add(locator); return this; }
        public Builder locators(Set<Locator> locators) { mLocators.addAll(locators); return this; }
        public Builder metadata(Metadata metadata) { mMetadata = metadata; return this; }
        public Builder timestamp(long timestamp) { mTimestamp = timestamp; return this; }

        public Ndo build() {
            return new Ndo(this);
        }

    }

    /** Log Tag. */
    public static final String TAG = Ndo.class.getSimpleName();

    /** Global Cache Folder. */
    public static final File CACHE_FOLDER = new File(Environment.getExternalStorageDirectory() + "/shared/");

    private String mAuthority;
    private String mAlgorithm;
    private String mHash;
    private Set<Locator> mLocators;
    private Metadata mMetadata;
    private long mTimestamp;
    private File mOctets;

    private Ndo(Builder builder) {
        mAuthority = builder.mAuthority;
        mAlgorithm = builder.mAlgorithm;
        mHash = builder.mHash;
        mLocators = Collections.unmodifiableSet(builder.mLocators);
        mMetadata = builder.mMetadata;
        mTimestamp = builder.mTimestamp;
        mOctets = new File(CACHE_FOLDER, builder.mHash);
    }

//    public Ndo(String algorithm, String hash) {
//        mAuthority = "";
//        mAlgorithm = algorithm;
//        mHash = hash;
//        mLocators = new LinkedHashSet<Locator>();
//        mMetadata = new Metadata();
//        mOctets = new File(CACHE_FOLDER, hash);
//    }
//
//    public Ndo(Ndo ndo) {
//        mAuthority = ndo.mAuthority;
//        mAlgorithm = ndo.mAlgorithm;
//        mHash = ndo.mHash;
//        mLocators = new LinkedHashSet<Locator>(ndo.mLocators);
//        mMetadata = new Metadata(ndo.mMetadata);
//        mOctets = ndo.mOctets;
//    }
//
//    /**
//     * Adds a {@link Locator} to the {@link Ndo}.
//     * @param locator
//     *     The {@link Locator}
//     */
//    public void addLocator(Locator locator) {
//        mLocators.add(locator);
//    }
//
//    public void addLocators(Set<Locator> locators) {
//        mLocators.addAll(locators);
//    }
//
//    /**
//     * Adds {@link Metadata} to the {@link Ndo}.
//     * @param metadata
//     *     The {@link Metadata} to add
//     */
//    public void addMetadata(Metadata metadata) {
//        mMetadata.merge(metadata);
//    }

    public void cache(File file) throws IOException {
        FileUtils.copyFile(file, mOctets);
    }

    public void cache(byte[] octets) throws IOException {
        FileUtils.writeByteArrayToFile(mOctets, octets);
    }

    public void cache(String data, String encoding) throws IOException {
        FileUtils.writeStringToFile(mOctets, data, encoding, false);
    }

    public FileOutputStream newCacheStream() throws FileNotFoundException {
        return new FileOutputStream(mOctets);
    }

    /**
     * Checks if the {@link Ndo} is cached locally.
     * @return
     *     True if cached locally, otherwise false
     */
    public boolean isCached() {
        return mOctets.exists();
    }

    /**
     * Checks if the {@link Ndo} matches some token(s).
     * @param tokens
     *     The tokens
     * @return
     *     True if the {@link Ndo} matches the tokens, otherwise false
     */
    public boolean matches(Set<String> tokens) {
        return mMetadata.matches(tokens);
    }

    /**
     * Gets the URI representing the {@link Ndo}.
     * @return
     *     The URI
     */
    public String getUri() {
        return "ni://" + mAuthority + "/" + mAlgorithm + ";" + mHash;
    }

    /**
     * Gets the canonical URI representing the {@link Ndo}.
     * @return
     *     The canonical URI
     */
    public String getCanonicalUri() {
        return "ni:///" + mAlgorithm + ";" + mHash;
    }

    public String getAuthority() {
        return mAuthority;
    }

    public String getAlgorithm() {
        return mAlgorithm;
    }

    public String getHash() {
        return mHash;
    }

    public Set<Locator> getLocators() {
        return mLocators;
    }

    public Metadata getMetadata() {
        return mMetadata;
    }

    public File getOctets() {
        return mOctets;
    }

//    public void setAuthority(String authority) {
//        mAuthority = authority;
//    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(TAG);
        builder.append("@");
        builder.append(Integer.toHexString(hashCode()));
        builder.append(" {");
        builder.append("auth=");
        builder.append(mAuthority);
        builder.append(", alg=");
        builder.append(mAlgorithm);
        builder.append(", hash=");
        builder.append(mHash);
        builder.append(", locators=");
        builder.append(mLocators);
        builder.append(", meta=");
        builder.append(mMetadata);
        builder.append(", cache=");
        if (isCached()) {
            try {
                builder.append(mOctets.getCanonicalPath());
            } catch (IOException e) {
                builder.append("ERROR");
            }
        } else {
            builder.append("NOT_CACHED");
        }
        builder.append("}");
        return builder.toString();
    }

    public boolean equals(Ndo ndo) {
        return (mAlgorithm == ndo.mAlgorithm) && (mHash == ndo.mHash);
    }

}
