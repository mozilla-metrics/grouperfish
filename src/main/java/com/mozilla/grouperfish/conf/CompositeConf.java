package com.mozilla.grouperfish.conf;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;
import com.mozilla.grouperfish.base.Assert;

class CompositeConf implements Conf {

	CompositeConf(Conf upper, Conf lower) {
		upper_ = upper;
		lower_ = lower;
	}

	CompositeConf(Conf me) {
		this(me, new Bottom());
	}

	@Override
	public String toJSON() {
		return JSONObject.toJSONString(asMap());
	}

	@Override
	public Map<String, ?> asMap() {
		Map<String, Object> composite = new HashMap<String, Object>();
		composite.putAll(lower_.asMap());
		return composite;
	}

	@Override
	public String get(String key) {
		if (upper_.get(key) != null)
			return upper_.get(key);
		if (lower_.get(key) != null)
			return lower_.get(key);
		Assert.unreachable("Fatal: Cannot lookup configuration for key %s", key);
		return null;
	}

	@Override
	public <C> List<C> getList(Class<C> type, String key) {
		List<C> upper = upper_.getList(type, key);
		if (upper != null)
			return upper;
		List<C> lower = lower_.getList(type, key);
		if (lower != null)
			return lower;
		return null;
	}

	static private class Bottom implements Conf {

		@Override
		public String toJSON() {
			return Assert.unreachable(String.class);
		}

		@Override
		public String get(String key) {
			return null;
		}

		@Override
		public <C> List<C> getList(Class<C> valueType, String type) {
			return null;
		}

		@Override
		public Map<? extends String, ?> asMap() {
			return Collections.emptyMap();
		}
	}

	private final Conf upper_;
	private final Conf lower_;

}
