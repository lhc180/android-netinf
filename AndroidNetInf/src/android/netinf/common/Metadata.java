package android.netinf.common;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

/**
 * Represents the metadata belonging to an {@link Ndo}.
 * @author Linus Sunde
 *
 */
public class Metadata implements Serializable {

    /** Log Tag. */
    public static final String TAG = Metadata.class.getSimpleName();

    /** Metadata (Should not contain outer "meta":{...}). */
    private JSONObject mMetadata = new JSONObject();

    /**
     * Creates a new empty {@link Metadata} object.
     */
    public Metadata() {
        mMetadata = new JSONObject();
    }

    /**
     * Creates a new {@link Metadata} object using a JSON string.
     * @param jsonString
     *     The JSON string. Metadata should be in the subsidiary JSON object "meta"
     * @throws NetInfException
     *     In case the JSON string is invalid
     */
    public Metadata(String jsonString) throws NetInfException {
        try {
            mMetadata = new JSONObject(jsonString);
        } catch (JSONException e) {
            throw new NetInfException("Failed to parse JSON string", e);
        }
    }

    /**
     * Creates a new {@link Metadata} object using a {@link JSONObject}.
     * @param jo
     *     The {@link JSONObject}
     * @throws JSONException
     */
    public Metadata(JSONObject jo) {
        try {
            mMetadata = new JSONObject(jo.toString());
        } catch (JSONException e) {
            Log.wtf("Failed to copy metadata", e);
        }
    }

    /**
     * Create a new {@link Metadata} object using another {@link Metadata}.
     * @param metadata
     *     The {@link Metadata}
     */
    public Metadata(Metadata metadata) {
        try {
            mMetadata = new JSONObject(metadata.mMetadata.toString());
        } catch (JSONException e) {
            Log.wtf("Failed to copy metadata", e);
        }
    }

    /**
     * Returns a {@link JSONObject} representation of the {@link Metadata}, including the "meta" field.
     * @return
     *     {@link JSONObject} representation
     */
    public JSONObject toJson() {
        try {
            return new JSONObject(mMetadata.toString());
        } catch (JSONException e) {
            // TODO figure out how to handle this in a good way
            Log.wtf("Failed to create JSONObject from metadata, returning empty JSONObject", e);
            return new JSONObject();
        }
    }

    public String toQuotedJsonString() {
        String quoted = JSONObject.quote(mMetadata.toString());
        return quoted.substring(1, quoted.length()-1);
    }

    public String toMetaString() {
        try {
            JSONObject jo = new JSONObject();
            jo.put("meta", mMetadata);
            return jo.toString();
        } catch (JSONException e) {
            // TODO figure out how to handle this in a good way
            Log.wtf("Failed to create meta string, returning {\"meta\":{}}", e);
            return "{\"meta\":{}}";
        }
    }

    /**
     * Returns a JSON {@link String} representation of the {@link Metadata}.
     * @return
     *     JSON {@link String} representation
     */
    @Override
    public String toString() {
        return mMetadata.toString();
    }

    /**
     * Returns a human readable JSON {@link String} representation of the {@link Metadata}.
     * @param indentSpaces
     *     The number of spaces to indent for each level of nesting.
     * @return
     *     Human readable JSON {@link String} representation
     */
    public String toString(int indentSpaces) {
        try {
            return mMetadata.toString(indentSpaces);
        } catch (JSONException e) {
            return "";
        }
    }

    /**
     * (NYI) Merges two {@link Metadata} objects.
     * @param metadata
     *     The {@link Metadata} object to merge with
     */
    public void merge(Metadata metadata) {
        // TODO merge instead of just updating
        mMetadata = metadata.mMetadata;
    }

    /**
     * Writes the {@link Metadata} object to an {@link ObjectOutputStream}.
     * @param out
     *     The {@link ObjectOutputStream}
     * @throws IOException
     *     In case an error occurs while writing to the target stream.
     */
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeObject(mMetadata.toString());
    }

    /**
     * Reads a {@link Metadata} object from an {@link ObjectInputStream}.
     * @param in
     *     The {@link ObjectInputStream}
     * @throws IOException
     *     In case an error occurs while reading from the {@link ObjectInputStream}
     * @throws ClassNotFoundException
     *     In case the class of the serialized object cannot be found
     */
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        try {
            mMetadata = new JSONObject((String) in.readObject());
        } catch (JSONException e) {
            throw new IOException("ObjectInputStream did not contain valid JSON", e);
        }
    }

    /**
     * Checks if the {@link Metadata} matches some token(s).
     * @param tokens
     *     The tokens
     * @return
     *     True if at least one {@link Metadata} value matches one of the tokens, otherwise false.
     */
    public boolean matches(Set<String> tokens) {
        return matches(mMetadata, new HashSet<String>(tokens));
    }

    /**
     * Checks if an {@link Object} matches some token(s).
     * The {@link Object} is assumed to be a {@link JSONObject}, {@link JSONArray}, or a subsidiary value of these.
     * @param object
     *     The {@link Object}
     * @param tokens
     *     The tokens
     * @return
     *     True if at least one subsidiary value matches one of the tokens, otherwise false.
     */
    private boolean matches(Object object, Set<String> tokens) {
        if (object instanceof String) {
            return tokens.contains(object);
        } else if (object instanceof JSONObject) {
            return matchesJsonObject((JSONObject) object, tokens);
        } else if (object instanceof JSONArray) {
            return matchesJsonArray((JSONArray) object, tokens);
        } else {
            Log.v(TAG, "Unhandled Object: " + object.getClass());
            return false;
        }
    }

    /**
     * Checks if a {@link JSONObject} matches some token(s)
     * @param current
     *     The {@link JSONObject}
     * @param tokens
     *     The tokens
     * @return
     *     True if at least one subsidiary value matches one of the tokens, otherwise false.
     */
    private boolean matchesJsonObject(JSONObject current, Set<String> tokens) {
        JSONArray names = current.names();
        if (names == null) {
            return false;
        }
        for (int i = 0; i < names.length(); i++) {
            try {
                String name = names.getString(i);
                Object next = current.get(name);
                if (matches(next, tokens)) {
                    return true;
                }
            } catch (JSONException e) {
                Log.wtf(TAG, "Failed to get next Object from JSONObject");
            }
        }
        return false;
    }

    /**
     * Checks if a {@link JSONArray} matches some tokens(s)
     * @param current
     *     The {@link JSONArray}
     * @param tokens
     *     The tokens
     * @return
     *     True if at least one subsidiary value matches one of the tokens, otherwise false.
     */
    private boolean matchesJsonArray(JSONArray current, Set<String> tokens) {
        for (int i = 0; i < current.length(); i++) {
            try {
                Object next = current.get(i);
                if (matches(next, tokens)) {
                    return true;
                }
            } catch (JSONException e) {
                Log.wtf(TAG, "Failed to get next Object from JSONArray");
            }
        }
        return false;
    }

}
