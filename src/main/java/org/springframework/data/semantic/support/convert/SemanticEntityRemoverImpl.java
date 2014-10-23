package org.springframework.data.semantic.support.convert;

import java.util.HashMap;

import org.openrdf.model.vocabulary.RDF;
import org.springframework.data.semantic.convert.SemanticEntityRemover;
import org.springframework.data.semantic.core.RDFState;
import org.springframework.data.semantic.core.SemanticDatabase;
import org.springframework.data.semantic.mapping.SemanticPersistentEntity;
import org.springframework.data.semantic.support.MappingPolicyImpl;
import org.springframework.data.semantic.support.convert.handlers.PropertiesToPatternsHandler;
import org.springframework.data.semantic.support.mapping.SemanticMappingContext;

public class SemanticEntityRemoverImpl implements SemanticEntityRemover {
	
	private SemanticDatabase semanticDb;
	private EntityToStatementsConverter toStatementsConverter;
	private SemanticMappingContext mappingContext;
	
	public SemanticEntityRemoverImpl(SemanticDatabase semanticDb, EntityToStatementsConverter toStatementsConverter, SemanticMappingContext mappingContext) {
		this.semanticDb = semanticDb;
		this.toStatementsConverter = toStatementsConverter;
		this.mappingContext = mappingContext;
	}

	@Override
	public <T> void delete(SemanticPersistentEntity<T> persistentEntity,
			T entity) {
		
		RDFState state = this.toStatementsConverter.convertEntityToDeleteStatements(persistentEntity, entity);
		this.semanticDb.removeStatements(state.getDeleteStatements());
	}

	@Override
	public <T> void deleteAll(SemanticPersistentEntity<T> persistentEntity) {
		String binding = "?uri";
		StringBuilder sb = new StringBuilder();
		sb.append("DELETE { ");
		sb.append(binding);
		sb.append(" <");
		sb.append(RDF.TYPE.stringValue());
		sb.append("> <");
		sb.append(persistentEntity.getRDFType().stringValue());
		sb.append("> . ");
		PropertiesToPatternsHandler handler = new PropertiesToPatternsHandler(sb, binding, new HashMap<String, Object>(), mappingContext, false, true, MappingPolicyImpl.DEFAULT_POLICY);
		persistentEntity.doWithProperties(handler);
		persistentEntity.doWithAssociations(handler);
		sb.append("} WHERE { ");
		sb.append(binding);
		sb.append(" <");
		sb.append(RDF.TYPE.stringValue());
		sb.append("> <");
		sb.append(persistentEntity.getRDFType().stringValue());
		sb.append("> }");
		
		this.semanticDb.executeUpdateStatement(sb.toString());
	}
	
	


}
