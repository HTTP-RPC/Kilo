/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.httprpc.kilo.sql;

/**
 * Represents a conditional operation.
 *
 * @param schemaElement
 * The schema element to which the left-hand side of the operation applies.
 *
 * @param operator
 * The operator.
 *
 * @param key
 * The key of the value to which the right-hand side of the operation applies.
 */
public record Conditional(SchemaElement schemaElement, Operator operator, String key) {
    /**
     * Represents a conditional operator.
     */
    public enum Operator {
        /**
         * The "=" operator.
         */
        EQUAL_TO("="),

        /**
         * The "!=" operator.
         */
        NOT_EQUAL_TO("!=");

        private final String value;

        Operator(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    /**
     * Constructs a new conditional.
     */
    public Conditional {
        if (schemaElement == null || operator == null || key == null) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Returns a string representation of the conditional.
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("%s %s :%s", schemaElement.label(), operator, key);
    }
}
