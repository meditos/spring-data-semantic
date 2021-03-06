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
package org.springframework.data.semantic.support.convert;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.springframework.data.semantic.mapping.MappingPolicy;
import org.springframework.data.semantic.mapping.SemanticPersistentEntity;
import org.springframework.data.semantic.mapping.SemanticPersistentProperty;
import org.springframework.data.semantic.support.MappingPolicyImpl;
import org.springframework.data.semantic.support.convert.handlers.AbstractPropertiesToQueryHandler;
import org.springframework.data.semantic.support.convert.handlers.PropertiesToBindingsHandler;
import org.springframework.data.semantic.support.convert.handlers.PropertiesToPatternsHandler;
import org.springframework.data.semantic.support.mapping.SemanticMappingContext;
import org.springframework.data.semantic.support.util.ValueUtils;
import org.springframework.util.StringUtils;

/**
 * 
 * @author konstantin.pentchev
 *
 */
public class EntityToQueryConverter {
	
	private static String variableChars = "abcdefghijklmnopqrstuvwxyz";
	
	private SemanticMappingContext mappingContext;
	
	public EntityToQueryConverter(SemanticMappingContext mappingContext){
		this.mappingContext = mappingContext;
	}
	
	
	/**
	 * Create a graph query retrieving a specific property of the entity identified by this uri
	 * @param uri 
	 * @param entity
	 * @param property
	 * @return
	 */
	public String getGraphQueryForResourceProperty(IRI uri, SemanticPersistentEntity<?> entity, SemanticPersistentProperty property){
		StringBuilder sb = new StringBuilder();
		
		sb.append("CONSTRUCT { ");
		sb.append(getPropertyBinding(uri, property));
		sb.append(" }\n");
		sb.append("WHERE { ");
		sb.append(getPropertyPattern(uri, entity, property));
		sb.append(" }");
		
		return sb.toString();
	}
	
	/**
	 * Create a graph query retrieving the molecule of an entity. 
	 * Only 'retrievable' {@link #isRetrivableProperty(SemanticPersistentProperty)} properties will be fetched
	 * @param uri - the uri of the entity
	 * @param entity - the container which holds the information about that entity
	 * @return
	 */
	public String getGraphQueryForResourceWithOriginalPredicates(IRI uri, SemanticPersistentEntity<?> entity, MappingPolicy globalMappingPolicy){
		return getGraphQueryForResource(uri, entity, new HashMap<String, Object>(), globalMappingPolicy, true);
	}
	
	/**
	 * Create a graph query retrieving the molecule of an entity. 
	 * Only 'retrievable' {@link #isRetrivableProperty(SemanticPersistentProperty)} properties will be fetched
	 * @param uri - the uri of the entity
	 * @param entity - the container which holds the information about that entity
	 * @return
	 */
	public String getGraphQueryForResource(IRI uri, SemanticPersistentEntity<?> entity, MappingPolicy globalMappingPolicy){
		return getGraphQueryForResource(uri, entity, new HashMap<String, Object>(), globalMappingPolicy, false);
	}
	
	/**
	 * Create a graph query retrieving the molecule of an entity. 
	 * Only 'retrievable' {@link #isRetrivableProperty(SemanticPersistentProperty)} properties will be fetched
	 * @param uri - the uri of the entity
	 * @param entity - the container which holds the information about that entity
	 * @param propertiesToValues - the properties with their required values
	 * @return
	 */
	public String getGraphQueryForResource(IRI uri, SemanticPersistentEntity<?> entity, Map<String, Object> propertyToValue, MappingPolicy globalMappingPolicy, Boolean originalPredicates){
		StringBuilder sb = new StringBuilder();
		
		sb.append("CONSTRUCT { ");
		sb.append(getPropertyBindings(uri, entity, propertyToValue, globalMappingPolicy, originalPredicates));
		sb.append(" }\n");
		sb.append("WHERE { ");
		sb.append(getPropertyPatterns(uri, entity, propertyToValue, false, globalMappingPolicy, true));
		sb.append(" }");
		return sb.toString();
	}
	
	/**
	 * Create a select count query for a given entity type.
	 * @param entity
	 * @return
	 */
	public String getGraphQueryForResourceCount(SemanticPersistentEntity<?> entity, Map<String, Object> propertyToValue){
		StringBuilder sb = new StringBuilder();
		String subjectBinding  = getSubjectBinding(null, entity);
		sb.append("SELECT (COUNT (DISTINCT "+subjectBinding+") as ?count) WHERE { "+subjectBinding+" a <"+entity.getRDFType()+"> . ");
		sb.append(getPropertyPatterns(null, entity, propertyToValue, true, MappingPolicyImpl.ALL_POLICY, false));
		sb.append("}");
		return sb.toString();
	}
	
	/**
	 * Create an ask query checking if an entity exists.
	 * @param resourceId
	 * @param entity
	 * @return
	 */
	public String getQueryForResourceExistence(IRI resourceId, SemanticPersistentEntity<?> entity){
		return "ASK {<"+resourceId+"> a <"+entity.getRDFType()+"> }";
	}
	
	/**
	 * Create a select query for the ids (IRIs) of entities of a given type in the given range.
	 * @param entity
	 * @param offset
	 * @param size
	 * @return
	 */
	public String getQueryForIds(SemanticPersistentEntity<?> entity, int offset, int size){
		return "SELECT ?id WHERE { ?id a <"+entity.getRDFType()+"> } OFFSET "+offset+" LIMIT "+size+"";
	}
	
	public String getGraphQueryForEntityClass(SemanticPersistentEntity<?> entity){
		return getGraphQueryForEntityClass(entity, new HashMap<String, Object>());
	}
	
	public String getGraphQueryForEntityClass(SemanticPersistentEntity<?> entity, Map<String, Object> propertyToValue){
		StringBuilder sb = new StringBuilder();
		
		sb.append("CONSTRUCT { ");
		sb.append(getPropertyBindings(null, entity, propertyToValue, MappingPolicyImpl.ALL_POLICY, false));
		sb.append(" }\n");
		sb.append("WHERE { ");
		sb.append(getPropertyPatterns(null, entity, propertyToValue, false, MappingPolicyImpl.ALL_POLICY, false));
		sb.append(" }");
		
		return sb.toString();
	}
	
	private String getSubjectBinding(IRI uri, SemanticPersistentEntity<?> entity){
		return uri != null ? "<"+uri+">" : "?"+entity.getRDFType().getLocalName();
	}
	
	/**
	 * Get bindings for the retrievable properties of the entity
	 * @param uri - the uri of the entity
	 * @param entity - the container holding the information about the entity's structure
	 * @return
	 */
	protected String getPropertyBindings(IRI uri, SemanticPersistentEntity<?> entity, Map<String, Object> propertyToValue, MappingPolicy globalMappingPolicy, Boolean originalPredicates){
		StringBuilder sb = new StringBuilder();
		String subjectBinding = getSubjectBinding(uri, entity);
		AbstractPropertiesToQueryHandler.appendPattern(sb, subjectBinding, "a", "<"+entity.getRDFType()+">");
		PropertiesToBindingsHandler handler = new PropertiesToBindingsHandler(sb, subjectBinding, propertyToValue, this.mappingContext, globalMappingPolicy, originalPredicates);
		entity.doWithProperties(handler);
		entity.doWithAssociations(handler);
		return sb.toString();
	}
	
	/**
	 * Create a binding for a specific property of an entity
	 * @param uri
	 * @param property
	 * @return
	 */
	protected static String getPropertyBinding(IRI uri, SemanticPersistentProperty property){
		StringBuilder sb = new StringBuilder();
		AbstractPropertiesToQueryHandler.appendPattern(sb, "<"+uri+">", "<" + property.getAliasPredicate() + ">", "?"+property.getName());
		return sb.toString();
	}
	
	/**
	 * Create a pattern matching a specific property of an entity
	 * @param uri
	 * @param entity
	 * @param property
	 * @return
	 */
	protected String getPropertyPattern(IRI uri, SemanticPersistentEntity<?> entity, SemanticPersistentProperty property){
		StringBuilder sb = new StringBuilder();
		new PropertiesToPatternsHandler(sb, "<"+uri+">", new HashMap<String, Object>(), this.mappingContext, false, false, MappingPolicyImpl.ALL_POLICY).doWithPersistentProperty(property);
		return sb.toString();
	}
	
	protected String getPropertyPatterns(IRI uri, SemanticPersistentEntity<?> entity, Map<String, Object> propertyToValue, boolean isCount, MappingPolicy globalMappingPolicy, boolean useUnions){
		StringBuilder sb = new StringBuilder();
		/*SemanticPersistentProperty contextP = entity.getContextProperty();
		if(contextP != null){
			sb.append("GRAPH ");
			//sb.append(contextP.); TODO
			sb.append("{ ");
		}*/
		String binding;
		if(uri != null){
			binding = "<"+uri.stringValue()+">";
		}
		else{
			binding = "?"+entity.getRDFType().getLocalName();
		}
        if(useUnions){
            sb.append("{ ");
        }
		AbstractPropertiesToQueryHandler.appendPattern(sb, binding, "<"+ValueUtils.RDF_TYPE_PREDICATE+">", "<"+entity.getRDFType()+">");
		if(useUnions){
            sb.append("} ");
        }
        PropertiesToPatternsHandler handler = new PropertiesToPatternsHandler(sb, binding, propertyToValue, this.mappingContext, isCount, false, globalMappingPolicy, useUnions);
		entity.doWithProperties(handler);
		entity.doWithAssociations(handler);
		return sb.toString();
	}
	
	protected String getVar(int input){
		int alphabetSize = variableChars.length();
		int result = input / alphabetSize;
		int remainder = input % alphabetSize;
		LinkedList<Character> var = new LinkedList<Character>();
		var.add(variableChars.charAt(remainder));
		while(result > 0){
			remainder = result % alphabetSize;
			var.addFirst(variableChars.charAt(remainder));
			result /= alphabetSize;
		}
		return StringUtils.collectionToDelimitedString(var, "");
	}
	
	
	
	

}
