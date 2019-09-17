/*
 *
 *  * Copyright 2014 http://Bither.net
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.bankledger.safecold.xrandom;


import android.util.Log;

import com.bankledger.safecoldj.utils.Sha256Hash;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;

public class UEntropyCollector implements IUEntropy, IUEntropySource {

    private final UEntropyCamera uEntropyCamera;

    public interface UEntropyCollectorListener {
        void onUEntropySourceError(Exception e, IUEntropySource source);
    }

    private UEntropyCollectorListener listener;


    public UEntropyCollector(UEntropyCamera uEntropyCamera, UEntropyCollectorListener listener) {
        this.listener = listener;
        this.uEntropyCamera = uEntropyCamera;
    }

    public void onError(Exception e, IUEntropySource source) {
        uEntropyCamera.onPause();

        if (listener != null) {
            listener.onUEntropySourceError(e, source);
        }
    }

    @Override
    public byte[] nextBytes(int length) {
        byte[] newestData = uEntropyCamera.getNewestData();
        byte[] processData = processData(newestData, length * length);
        byte[] bytes = null;
        for (int i = 0; i < length; i++) {
            byte[] itemBytes = new byte[length];
            System.arraycopy(processData, length * i, itemBytes, 0, length);
            if (i == length - 1) {
                itemBytes = Sha256Hash.create(itemBytes).getBytes();
            }
            if (bytes == null) {
                bytes = itemBytes;
            } else {
                for (int k = 0; k < bytes.length && k < itemBytes.length; k++) {
                    bytes[k] = (byte) (bytes[k] ^ itemBytes[k]);
                }
            }
        }
        return bytes;
    }


    public byte[] processData(byte[] data, int targetLen) {
        if (data.length <= targetLen) {
            return data;
        }
        byte[] result = new byte[targetLen];
        byte[] locatorBytes;
        for (int i = 0; i < targetLen - 1; i++) {
            int position = (int) (Math.random() * data.length);
            try {
                locatorBytes = URandom.nextBytes(Ints.BYTES);
                int value = Math.abs(Ints.fromByteArray(locatorBytes));
                position = (int) (((float) value / (float) Integer.MAX_VALUE) * data.length);
            } catch (Exception e) {
                e.printStackTrace();
            }
            position = Math.min(Math.max(position, 0), data.length - 1);
            result[i] = data[position];
        }
        byte[] timestampBytes = Longs.toByteArray(System.currentTimeMillis());
        result[targetLen - 1] = timestampBytes[timestampBytes.length - 1];
        return result;
    }


    @Override
    public void onResume() {
        uEntropyCamera.onResume();
    }

    @Override
    public void onPause() {
        uEntropyCamera.onPause();
    }
}
