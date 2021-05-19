package org.nvo.service.validation;

import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Iterator;

/**
 * a class for managing a bit field of test result types.  This class defines
 * a standard set (PASS, WARN, REC, and FAIL), but others may be defined.  
 */
public class ResultTypes implements Cloneable {

    /**
     * result type indicating the service passed the test.
     */
    public final static int PASS = 1;

    /**
     * result type indicating the service failed the test.
     */
    public final static int FAIL = 2;
    
    /**
     * result type indicating the service produced a warning
     */
    public final static int WARN = 4;
    
    /**
     * result type indicating the service produced a recommendation
     */
    public final static int REC = 8;

    /**
     * result type combining FAIL, WARN, and REC
     */
    public final static int ADVICE = FAIL|WARN|REC;

    /**
     * result type combining PASS, FAIL, WARN, and REC
     */
    public final static int ALL = PASS|FAIL|WARN|REC;

    /**
     * a dictionary of result type definitions
     */
    public static class Dictionary {

        private HashMap lookup = null;
        private HashMap bytoken = null;

        /**
         * create an empty dictionary with an initial capacity
         */
        public Dictionary() {
            this(4);
        }

        /**
         * create an empty dictionary with an initial capacity
         */
        public Dictionary(int initCapacity) {
            lookup = new HashMap(initCapacity);
        }

        /**
         * create a dictionary
         * @param initCapacity  the initial capacity of the dictionary
         * @param defineStd     if true, define the standard types in English
         */
        public Dictionary(int initCapacity, boolean defineStd) {
            this(initCapacity);
            if (defineStd) defineStandardTypes();
        }

        /**
         * add a definition for an type
         * @param type   a type value.  This normally is an integer equal 
         *                to a power of 2; however, other values can be 
         *                used to define composite types.
         * @param token  a short string representation.  This is used as 
         *                an identifying token in an XML evaluation result
         *                and cannot be null.
         * @param name   a full title for the type that can be used in 
         *                displays.  If null, the value of token will be used.
         * @param desc   a short description of the result type.  Can be null.
         * @exception NullPointerException if token is null.
         */
        public void addType(int type, String token, String name, String desc) {
            if (token == null) throw new NullPointerException("token");
            if (name == null) name = token;
            if (desc == null) desc = "";

            String[] def = new String[] { token, name, desc };
            Integer t = new Integer(type);
            lookup.put(t, def);
            if (bytoken != null) {
                synchronized (bytoken) { bytoken.put(token, t); }
            }
        }

        private String getDef(int type, int item) {
            String[] out = (String[]) lookup.get(new Integer(type));
            if (out == null) return null;
            return out[item];
        }

        /**
         * return the token associated with the given result type.  Null
         * is returned if the type is undefined.
         */
        public final String getToken(int type) {
            return getDef(type, 0);
        }

        /**
         * return the full name of the given result type.  Null
         * is returned if the type is undefined.
         */
        public final String getName(int type) { return getDef(type, 1); }

        /**
         * return a short description of the given result type.  Null
         * is returned if the type is undefined.
         */
        public final String getDescription(int type) { 
            return getDef(type, 2); 
        }

        /**
         * return the type for a given token 
         */
        public final int getTypeByToken(String token) {
            if (bytoken == null) {
                bytoken = new HashMap(lookup.size());
                synchronized (bytoken) {
                    Integer key = null;
                    Iterator ki = lookup.keySet().iterator();
                    while (ki.hasNext()) {
                        key = (Integer) ki.next();
                        bytoken.put(getToken(key.intValue()), key);
                    }
                }
            }
            if (bytoken.containsKey(token)) 
                return ((Integer) bytoken.get(token)).intValue();
            else 
                return -1;
        }

        /**
         * define the standard result types.  This default implementation
         * provides English definitions.
         */
        protected void defineStandardTypes() {
            addType(ResultTypes.PASS, "pass", "Successes", 
                    "Tests that show full compliance");
            addType(ResultTypes.FAIL, "fail", "Failures", 
                    "Tests that have failed to find minimal compliance");
            addType(ResultTypes.WARN, "warn", "Warnings", 
                    "Tests that demonstrate minimal compliance but show " +
                    "potential problems");
            addType(ResultTypes.REC, "rec", "Recommendations", 
                    "Tests that result recommendations for changes");
        }
    }

    private static ResultTypes.Dictionary stdDict = 
        new ResultTypes.Dictionary(4, true);
    ResultTypes.Dictionary dict = null;
    private int types = 0;

    /**
     * create an empty result types with a default dictionary.
     */
    public ResultTypes() {
        this(0);
    }

    /**
     * create an empty result types with a default dictionary.
     * @param types   an OR-ed set of result types to initialize to
     */
    public ResultTypes(int types) {
        this(stdDict, types);
    }

    /**
     * create an empty result types with a default dictionary.
     * @param types   an OR-ed set of result types to initialize to
     */
    public ResultTypes(ResultTypes.Dictionary dictionary, int types) {
        dict = dictionary;
        this.types = types;
    }

    /**
     * create an empty result types with the same dictionary as the 
     * given object
     */
    public ResultTypes(ResultTypes like) {
        this(like.dict, 0);
    }

    /**
     * return the currently set result types 
     */
    public int getTypes() { return types; }

    /**
     * get result type tokens as a space-delimited concatonation of the 
     * tokens.
     */
    public String getTypeTokens() {
        int types = getTypes();
        StringBuffer sb = new StringBuffer();
        for(int m=0, t=1; (types & (~m)) > 0; t=t<<1) {
            if ((types & t) > 0) {
                String token = getToken(t);
                m |= t;
                if (token != null) {
                    sb.append(token);
                    if ((types & (~m)) > 0) sb.append(' ');
                }
            }
        }
        return sb.toString();
    }

    /**
     * set the result types
     * @param types   the types OR-ed together
     */
    public void setTypes(int types) { this.types = types; }

    /**
     * set the result types
     * @param tokens   the types provided as a space-delimited set of tokens
     */
    public void setTypes(String tokens) { 
        setTypes(0);
        addTypes(tokens);
    }

    /**
     * add the result types to the current set
     * @param types   the types OR-ed together to add
     */
    public void addTypes(int types) { this.types |= types; }

    /**
     * add the result types to the current set
     * @param tokens   the types provided as a space-delimited set of tokens
     */
    public void addTypes(String tokens) { 
        int t = 0;
        StringTokenizer st = new StringTokenizer(tokens);
        while (st.hasMoreTokens()) {
            t = dict.getTypeByToken(st.nextToken());
            if (t > 0) addTypes(t);
        }
    }

    /**
     * return the token associated with the given result type.  Null
     * is returned if the type is undefined.
     */
    public final String getToken(int type) { return dict.getToken(type); }

    /**
     * return the full name of the given result type.  Null
     * is returned if the type is undefined.
     */
    public final String getName(int type) { return dict.getName(type); }

    /**
     * return a short description of the given result type.  Null
     * is returned if the type is undefined.
     */
    public final String getDescription(int type) { 
        return dict.getDescription(type); 
    }

    /**
     * clone this collection.  The same dictionary will be shared
     */
    public Object clone() {
        try {
            return super.clone();
        }
        catch (CloneNotSupportedException ex) {
            throw new InternalError("programmer clone error");
        }
    }
}
