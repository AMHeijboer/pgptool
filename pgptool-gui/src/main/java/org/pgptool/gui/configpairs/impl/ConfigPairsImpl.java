/*******************************************************************************
 * PGPTool is a desktop application for pgp encryption/decryption
 * Copyright (C) 2019 Sergey Karpushin
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package org.pgptool.gui.configpairs.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.pgptool.gui.config.api.ConfigRepository;
import org.pgptool.gui.configpairs.api.ConfigPairs;
import org.pgptool.gui.usage.api.UsageLogger;
import org.pgptool.gui.usage.dto.ConfigPairUsage;
import org.springframework.beans.factory.annotation.Autowired;
import org.summerb.approaches.jdbccrud.api.dto.EntityChangedEvent;
import org.summerb.approaches.jdbccrud.common.DtoBase;

import com.google.common.eventbus.EventBus;

/**
 * This is VERY simple map-based impl of this storage. It uses config repo and
 * performs write operation each time after single key-value pair change
 * 
 * @author Sergey Karpushin
 *
 */
public class ConfigPairsImpl implements ConfigPairs {
	@Autowired
	private ConfigRepository configRepository;
	@Autowired
	private EventBus eventBus;
	@Autowired
	private UsageLogger usageLogger;

	private ConfigPairsEnvelop configPairsEnvelop;

	private String clarification;

	public ConfigPairsImpl(String clarification) {
		this.clarification = clarification;
	}

	@Override
	public synchronized void put(String key, Object value) {
		if (value == null) {
			Object removed = getConfigPairsEnvelop().remove(key);
			if (removed != null & removed instanceof DtoBase) {
				eventBus.post(EntityChangedEvent.removedObject((DtoBase) removed));
			}
		} else {
			Object previous = getConfigPairsEnvelop().put(key, value);

			if (previous != null & value instanceof DtoBase) {
				eventBus.post(EntityChangedEvent.updated((DtoBase) value));
			} else if (value != null & value instanceof DtoBase) {
				eventBus.post(EntityChangedEvent.added((DtoBase) value));
			}
		}
		save();
		usageLogger.write(new ConfigPairUsage(clarification, key, value));
	}

	private void save() {
		configRepository.persist(getConfigPairsEnvelop(), clarification);
	}

	@SuppressWarnings("unchecked")
	@Override
	public synchronized <T> T find(String key, T defaultValue) {
		T ret = (T) getConfigPairsEnvelop().get(key);
		return ret != null ? ret : defaultValue;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> List<T> findAllWithPrefixedKey(String keyPrefix) {
		List<T> ret = new ArrayList<>();
		for (Entry<String, Object> entry : getConfigPairsEnvelop().entrySet()) {
			if (entry.getKey().startsWith(keyPrefix)) {
				ret.add((T) entry.getValue());
			}
		}
		return ret;
	}

	@Override
	public synchronized Set<Entry<String, Object>> getAll() {
		return new HashSet<>(getConfigPairsEnvelop().entrySet());
	}

	public ConfigPairsEnvelop getConfigPairsEnvelop() {
		if (configPairsEnvelop == null) {
			configPairsEnvelop = configRepository.readOrConstruct(ConfigPairsEnvelop.class, clarification);
		}
		return configPairsEnvelop;
	}
}
