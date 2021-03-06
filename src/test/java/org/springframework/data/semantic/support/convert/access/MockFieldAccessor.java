/**
 * Copyright (C) 2014 Ontotext AD (info@ontotext.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.semantic.support.convert.access;

import org.springframework.data.semantic.convert.access.FieldAccessor;
import org.springframework.data.semantic.mapping.MappingPolicy;

public class MockFieldAccessor implements FieldAccessor{

	@Override
	public Object getDefaultValue() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object setValue(Object entity, Object newVal, MappingPolicy mappingPolicy) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getValue(Object entity, MappingPolicy mappingPolicy) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isWritable(Object entity) {
		// TODO Auto-generated method stub
		return false;
	}

}
