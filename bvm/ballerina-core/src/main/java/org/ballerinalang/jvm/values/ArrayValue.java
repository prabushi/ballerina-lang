/*
*  Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/
package org.ballerinalang.jvm.values;

import org.ballerinalang.bre.bvm.BVM;
import org.ballerinalang.jvm.freeze.State;
import org.ballerinalang.jvm.freeze.Status;
import org.ballerinalang.jvm.freeze.Utils;
import org.ballerinalang.model.types.BArrayType;
import org.ballerinalang.model.types.BTupleType;
import org.ballerinalang.model.types.BType;
import org.ballerinalang.model.types.BTypes;
import org.ballerinalang.model.types.TypeTags;
import org.ballerinalang.model.util.JsonGenerator;
import org.ballerinalang.util.BLangConstants;
import org.ballerinalang.util.exceptions.BLangExceptionHelper;
import org.ballerinalang.util.exceptions.BallerinaErrorReasons;
import org.ballerinalang.util.exceptions.BallerinaException;
import org.ballerinalang.util.exceptions.RuntimeErrors;
import org.wso2.ballerinalang.compiler.util.BArrayState;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;


/**
 * Represent an array in ballerina.
 * @since 0.995.0
 */
public class ArrayValue implements RefValue {

    protected BType arrayType;
    private volatile Status freezeStatus = new Status(State.UNFROZEN);

    /**
     * The maximum size of arrays to allocate.
     * <p>
     * This is same as Java
     */
    protected int maxArraySize = Integer.MAX_VALUE - 8;
    private static final int DEFAULT_ARRAY_SIZE = 100;
    protected int size = 0;

    RefValue[] refValues;
    private long[] intValues;
    private int[] booleanValues;
    private byte[] byteValues;
    private double[] floatValues;
    private String[] stringValues;

    public BType elementType;

    //------------------------ Constructors -------------------------------------------------------------------

    public ArrayValue(RefValue[] values, BType type) {
        this.refValues = values;
        this.arrayType = type;
        this.size = values.length;
    }

    public ArrayValue(long[] values) {
        this.intValues = values;
        this.size = values.length;
        setArrayElementType(BTypes.typeInt);
    }

    public ArrayValue(int[] values) {
        this.booleanValues = values;
        this.size = values.length;
        setArrayElementType(BTypes.typeBoolean);
    }

    public ArrayValue(byte[] values) {
        this.byteValues = values;
        this.size = values.length;
        setArrayElementType(BTypes.typeByte);
    }

    public ArrayValue(double[] values) {
        this.floatValues = values;
        this.size = values.length;
        setArrayElementType(BTypes.typeFloat);
    }

    public ArrayValue(String[] values) {
        this.stringValues = values;
        this.size = values.length;
        setArrayElementType(BTypes.typeString);
    }

    public ArrayValue(BType type) {
        if (type.getTag() == TypeTags.INT_TAG) {
            intValues = (long[]) newArrayInstance(Long.TYPE);
            setArrayElementType(type);
        } else if (type.getTag() == TypeTags.BOOLEAN_TAG) {
            booleanValues = (int[]) newArrayInstance(Integer.TYPE);
            setArrayElementType(type);
        } else if (type.getTag() == TypeTags.BYTE_TAG) {
            byteValues = (byte[]) newArrayInstance(Byte.TYPE);
            setArrayElementType(type);
        } else if (type.getTag() == TypeTags.FLOAT_TAG) {
            floatValues = (double[]) newArrayInstance(Double.TYPE);
            setArrayElementType(type);
        } else if (type.getTag() == TypeTags.STRING_TAG) {
            stringValues = (String[]) newArrayInstance(String.class);
            Arrays.fill(stringValues, BLangConstants.STRING_EMPTY_VALUE);
            setArrayElementType(type);
        } else {
            this.arrayType = type;
            if (type.getTag() == TypeTags.ARRAY_TAG) {
                BArrayType arrayType = (BArrayType) type;
                this.elementType = arrayType.getElementType();
                if (arrayType.getState() == BArrayState.CLOSED_SEALED) {
                    this.size = maxArraySize = arrayType.getSize();
                }
                refValues = (RefValue[]) newArrayInstance(RefValue.class);
                Arrays.fill(refValues, arrayType.getElementType().getZeroValue());
            } else if (type.getTag() == TypeTags.TUPLE_TAG) {
                BTupleType tupleType = (BTupleType) type;
                this.size = maxArraySize = tupleType.getTupleTypes().size();
                refValues = (RefValue[]) newArrayInstance(RefValue.class);
                AtomicInteger counter = new AtomicInteger(0);
                tupleType.getTupleTypes().forEach(memType ->
                        refValues[counter.getAndIncrement()] = memType.getEmptyValue());
            } else {
                refValues = (RefValue[]) newArrayInstance(RefValue.class);
                Arrays.fill(refValues, type.getEmptyValue());
            }
        }
    }

    public ArrayValue() {
        refValues = (RefValue[]) newArrayInstance(RefValue.class);
    }

    public ArrayValue(BType type, int size) {
        if (size != -1) {
            this.size = maxArraySize = size;
        }

        if (type.getTag() == TypeTags.INT_TAG) {
            intValues = (long[]) newArrayInstance(Long.TYPE);
        } else if (type.getTag() == TypeTags.BOOLEAN_TAG) {
            booleanValues = (int[]) newArrayInstance(Integer.TYPE);
        } else if (type.getTag() == TypeTags.BYTE_TAG) {
            byteValues = (byte[]) newArrayInstance(Byte.TYPE);
        } else if (type.getTag() == TypeTags.FLOAT_TAG) {
            floatValues = (double[]) newArrayInstance(Double.TYPE);
        } else if (type.getTag() == TypeTags.STRING_TAG) {
            stringValues = (String[]) newArrayInstance(String.class);
            Arrays.fill(stringValues, BLangConstants.STRING_EMPTY_VALUE);
        }

        this.arrayType = new BArrayType(type, size);
        this.elementType = type;
    }

    // -----------------------  get methods ----------------------------------------------------

    public RefValue getRefValue(long index) {
        rangeCheckForGet(index, size);
        return refValues[(int) index];
    }

    public long getInt(long index) {
        rangeCheckForGet(index, size);
        return intValues[(int) index];
    }

    public int getBoolean(long index) {
        rangeCheckForGet(index, size);
        return booleanValues[(int) index];
    }

    public byte getByte(long index) {
        rangeCheckForGet(index, size);
        return byteValues[(int) index];
    }

    public double getFloat(long index) {
        rangeCheckForGet(index, size);
        return floatValues[(int) index];
    }

    public String getString(long index) {
        rangeCheckForGet(index, size);
        return stringValues[(int) index];
    }

    // ----------------------------  add methods --------------------------------------------------

    public void add(long index, RefValue value) {
        handleFrozenArrayValue();
        prepareForAdd(index, refValues.length);
        refValues[(int) index] = value;
    }

    public void add(long index, long value) {
        handleFrozenArrayValue();
        prepareForAdd(index, intValues.length);
        intValues[(int) index] = value;
    }

    public void add(long index, int value) {
        if (elementType.getTag() == TypeTags.INT_TAG) {
            add(index, (long) value);
            return;
        }

        handleFrozenArrayValue();
        prepareForAdd(index, booleanValues.length);
        booleanValues[(int) index] = value;
    }

    public void add(long index, byte value) {
        handleFrozenArrayValue();
        prepareForAdd(index, byteValues.length);
        byteValues[(int) index] = value;
    }

    public void add(long index, double value) {
        handleFrozenArrayValue();
        prepareForAdd(index, floatValues.length);
        floatValues[(int) index] = value;
    }

    public void add(long index, String value) {
        handleFrozenArrayValue();
        prepareForAdd(index, stringValues.length);
        stringValues[(int) index] = value;
    }

    //-------------------------------------------------------------------------------------------------------------

    public void append(RefValue value) {
        add(size, value);
    }

    @Override
    public BType getType() {
        return elementType;
    }

    @Override
    public void stamp(BType type) {

    }

    public void stamp(BType type, List<BVM.TypeValuePair> unresolvedValues) {

    }

    @Override
    public RefValue copy(Map<RefValue, RefValue> refs) {
        if (isFrozen()) {
            return this;
        }

        if (refs.containsKey(this)) {
            return refs.get(this);
        }

        if (elementType != null) {
            ArrayValue valueArray = null;

            if (elementType.getTag() == TypeTags.INT_TAG) {
                valueArray = new ArrayValue(Arrays.copyOf(intValues, intValues.length));
            } else if (elementType.getTag() == TypeTags.BOOLEAN_TAG) {
                valueArray = new ArrayValue(Arrays.copyOf(booleanValues, booleanValues.length));
            } else if (elementType.getTag() == TypeTags.BYTE_TAG) {
                valueArray = new ArrayValue(Arrays.copyOf(byteValues, byteValues.length));
            } else if (elementType.getTag() == TypeTags.FLOAT_TAG) {
                valueArray = new ArrayValue(Arrays.copyOf(floatValues, floatValues.length));
            } else if (elementType.getTag() == TypeTags.STRING_TAG) {
                valueArray = new ArrayValue(Arrays.copyOf(stringValues, stringValues.length));
            }

            if (valueArray != null) {
                valueArray.size = this.size;
                refs.put(this, valueArray);
                return valueArray;
            }
        }

        RefValue[] values = new RefValue[size];
        ArrayValue refValueArray = new ArrayValue(values, arrayType);
        refValueArray.size = this.size;
        refs.put(this, refValueArray);
        int bound = this.size;
        IntStream.range(0, bound)
                .forEach(i -> values[i] = this.refValues[i] == null ? null :
                        (RefValue) this.refValues[i].copy(refs));
        return refValueArray;

    }

    @Override
    public String toString() {
        if (elementType != null) {
            StringJoiner sj = new StringJoiner(", ", "[", "]");
            if (elementType.getTag() == TypeTags.INT_TAG) {
                for (int i = 0; i < size; i++) {
                    sj.add(Long.toString(intValues[i]));
                }
                return sj.toString();
            } else if (elementType.getTag() == TypeTags.BOOLEAN_TAG) {
                for (int i = 0; i < size; i++) {
                    sj.add(Boolean.toString(booleanValues[i] == 1));
                }
                return sj.toString();
            } else if (elementType.getTag() == TypeTags.BYTE_TAG) {
                for (int i = 0; i < size; i++) {
                    sj.add(Integer.toString(Byte.toUnsignedInt(byteValues[i])));
                }
                return sj.toString();
            } else if (elementType.getTag() == TypeTags.FLOAT_TAG) {
                for (int i = 0; i < size; i++) {
                    sj.add(Double.toString(floatValues[i]));
                }
                return sj.toString();
            } else if (elementType.getTag() == TypeTags.STRING_TAG) {
                for (int i = 0; i < size; i++) {
                    sj.add("\"" + stringValues[i] + "\"");
                }
                return sj.toString();
            }
        }

        if (getElementType(arrayType).getTag() == TypeTags.JSON_TAG) {
            return getJSONString();
        }

        StringJoiner sj;
        if (arrayType != null && (arrayType.getTag() == TypeTags.TUPLE_TAG)) {
            sj = new StringJoiner(", ", "(", ")");
        } else {
            sj = new StringJoiner(", ", "[", "]");
        }

        for (int i = 0; i < size; i++) {
            if (refValues[i] != null) {
                sj.add((refValues[i].getType().getTag() == TypeTags.STRING_TAG)
                        ? ("\"" + refValues[i] + "\"") : refValues[i].toString());
            }
        }
        return sj.toString();
    }

    public RefValue[] getValues() {
        return refValues;
    }

    public byte[] getBytes() {
        return byteValues.clone();
    }

    @SuppressWarnings("unchecked")
    public String[] getStringArray() {
        return stringValues;
    }

    @Override
    public void serialize(OutputStream outputStream) {
        if (elementType.getTag() == TypeTags.BYTE_TAG) {
            try {
                outputStream.write(byteValues);
            } catch (IOException e) {
                throw new BallerinaException("error occurred while writing the binary content to the output stream", e);
            }
        } else {
            try {
                outputStream.write(this.toString().getBytes(Charset.defaultCharset()));
            } catch (IOException e) {
                throw new BallerinaException("error occurred while serializing data", e);
            }
        }
    }

    public void grow(int newLength) {
        if (elementType != null) {
            switch (elementType.getTag()) {
                case TypeTags.INT_TAG:
                    intValues = Arrays.copyOf(intValues, newLength);
                    break;
                case TypeTags.BOOLEAN_TAG:
                    booleanValues = Arrays.copyOf(booleanValues, newLength);
                    break;
                case TypeTags.BYTE_TAG:
                    byteValues = Arrays.copyOf(byteValues, newLength);
                    break;
                case TypeTags.FLOAT_TAG:
                    floatValues = Arrays.copyOf(floatValues, newLength);
                    break;
                case TypeTags.STRING_TAG:
                    stringValues = Arrays.copyOf(stringValues, newLength);
                    break;
                default:
                    refValues = Arrays.copyOf(refValues, newLength);
                    break;
            }
        } else {
            refValues = Arrays.copyOf(refValues, newLength);
        }
    }

    public BType getArrayType() {
        return arrayType;
    }

    private void rangeCheckForGet(long index, int size) {
        rangeCheck(index, size);
        if (index < 0 || index >= size) {
            throw BLangExceptionHelper.getRuntimeException(BallerinaErrorReasons.INDEX_OUT_OF_RANGE_ERROR,
                    RuntimeErrors.ARRAY_INDEX_OUT_OF_RANGE, index, size);
        }
    }

    private void rangeCheck(long index, int size) {
        if (index > Integer.MAX_VALUE || index < Integer.MIN_VALUE) {
            throw BLangExceptionHelper.getRuntimeException(BallerinaErrorReasons.INDEX_OUT_OF_RANGE_ERROR,
                    RuntimeErrors.INDEX_NUMBER_TOO_LARGE, index);
        }

        if ((int) index < 0 || index >= maxArraySize) {
            throw BLangExceptionHelper.getRuntimeException(BallerinaErrorReasons.INDEX_OUT_OF_RANGE_ERROR,
                    RuntimeErrors.ARRAY_INDEX_OUT_OF_RANGE, index, size);
        }
    }

    private Object newArrayInstance(Class<?> componentType) {
        return (size > 0) ?
                Array.newInstance(componentType, size) : Array.newInstance(componentType, DEFAULT_ARRAY_SIZE);
    }

    private void setArrayElementType(BType type) {
        this.arrayType = new BArrayType(type);
        this.elementType = type;
    }

    private String getJSONString() {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        JsonGenerator gen = new JsonGenerator(byteOut);
        try {
//            TODO
//            gen.serialize(this);
            gen.flush();
        } catch (IOException e) {
            throw new BallerinaException("Error in converting JSON to a string: " + e.getMessage(), e);
        }
        return new String(byteOut.toByteArray());
    }

    private BType getElementType(BType type) {
        if (type.getTag() != TypeTags.ARRAY_TAG) {
            return type;
        }

        return getElementType(((BArrayType) type).getElementType());
    }

    /**
     * Util method to handle frozen array values.
     */
    private void handleFrozenArrayValue() {
        synchronized (this) {
            if (this.freezeStatus.getState() != State.UNFROZEN) {
                Utils.handleInvalidUpdate(freezeStatus.getState());
            }
        }
    }


    protected void prepareForAdd(long index, int currentArraySize) {
        int intIndex = (int) index;
        rangeCheck(index, size);
        ensureCapacity(intIndex + 1, currentArraySize);
        resetSize(intIndex);
    }

    private void ensureCapacity(int requestedCapacity, int currentArraySize) {
        if ((requestedCapacity) - currentArraySize >= 0) {
            // Here the growth rate is 1.5. This value has been used by many other languages
            int newArraySize = currentArraySize + (currentArraySize >> 1);

            // Now get the maximum value of the calculate new array size and request capacity
            newArraySize = Math.max(newArraySize, requestedCapacity);

            // Now get the minimum value of new array size and maximum array size
            newArraySize = Math.min(newArraySize, maxArraySize);
            grow(newArraySize);
        }
    }

    private void resetSize(int index) {
        if (index >= size) {
            size = index + 1;
        }
    }
}
