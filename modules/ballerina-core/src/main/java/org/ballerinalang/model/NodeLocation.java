/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.ballerinalang.model;

import org.ballerinalang.model.values.Position;

/**
 * {@code NodeLocation} represents the location of a particular language construct in the source file.
 *
 * @since 0.8.0
 */
public class NodeLocation {
    private String pkgDirPath;
    private String fileName;
    private int lineNumber = -1;
    private Position startPosition;
    private Position stopPosition;

    public NodeLocation(String fileName, int lineNumber) {
        this.fileName = fileName;
        this.lineNumber = lineNumber;
    }

    public NodeLocation(String pkgDirPath, String fileName, int lineNumber) {
        this(fileName, lineNumber);
        this.pkgDirPath = pkgDirPath;
    }

    public NodeLocation(String pkgDirPath, String fileName, int lineNumber, Position startPosition,
                        Position stopPosition) {
        this(pkgDirPath, fileName, lineNumber);
        this.startPosition = startPosition;
        this.stopPosition = stopPosition;
    }

    public String getPackageDirPath() {
        return pkgDirPath;
    }

    public String getFileName() {
        return this.fileName;
    }

    public int getLineNumber() {
        return this.lineNumber;
    }

    public Position getStartPosition() {
        return startPosition;
    }

    public void setStartPosition(Position startPosition) {
        this.startPosition = startPosition;
    }

    public Position getStopPosition() {
        return stopPosition;
    }

    public void setStopPosition(Position stopPosition) {
        this.stopPosition = stopPosition;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof NodeLocation)) {
            return false;
        }

        NodeLocation other = (NodeLocation) obj;
        return (this.fileName.equals(other.getFileName()) && this.lineNumber == other.getLineNumber());
    }

    public int hashCode() {
        int result = this.fileName.hashCode() + lineNumber;
        result = 31 * result;
        return result;
    }

    @Override
    public String toString() {
        return fileName + ":" + lineNumber;
    }
}
