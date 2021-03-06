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
package org.springframework.data.semantic.support.cache;

import java.io.Serializable;

import org.eclipse.rdf4j.model.IRI;
import org.springframework.data.semantic.cache.EntityCache;
import org.springframework.data.semantic.mapping.SemanticPersistentEntity;
import org.springframework.data.semantic.support.mapping.SemanticMappingContext;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;

public class EhCacheEntityCache implements EntityCache {
	
	private CacheManager cacheManager;
	private SemanticMappingContext mappingContext;

	public EhCacheEntityCache(SemanticMappingContext mappingContext, CacheManager cacheManager) {
		this.mappingContext = mappingContext;
		this.cacheManager = cacheManager;
	}

	
	@Override
	public <T> void remove(T entity) {
		Ehcache cache = getCache(entity.getClass());
		cache.remove(getId(entity).stringValue());
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T get(IRI id, Class<? extends T> clazz) {
		Ehcache cache = getCache(clazz);
		Element element = cache.get(id.toString());
		if(element != null){
			Object value = element.getObjectValue();
			if(clazz.isAssignableFrom(value.getClass())){
				return (T) value;
			}
		}
		return null;
	}

	@Override
	public <T> void put(T entity) {
		if(entity != null && entity instanceof Serializable){
			Ehcache cache = getCache(entity.getClass());
			cache.put(new Element(getId(entity).toString(), entity));
		}
	}

	@Override
	public <T> void clear(Class<? extends T> clazz){
		Ehcache cache = getCache(clazz);
		cache.removeAll();
	}
	
	@Override
	public void clearAll() {
		cacheManager.clearAll();
	}
	
	private IRI getId(Object entity){
		SemanticPersistentEntity<?> persistentEntity = mappingContext.getPersistentEntity(entity.getClass());
		return persistentEntity.getResourceId(entity);
	}
	
	private Ehcache getCache(Class<?> clazz){
		String cacheName = clazz.getSimpleName();
		Ehcache cache = cacheManager.getCache(cacheName);
		if(cache == null){
			CacheConfiguration config = new CacheConfiguration(cacheName, 1000).copyOnRead(true).copyOnWrite(true);
			cache = new Cache(config);
			cacheManager.addCache(cache);
		}
		return cache;
	}

}
