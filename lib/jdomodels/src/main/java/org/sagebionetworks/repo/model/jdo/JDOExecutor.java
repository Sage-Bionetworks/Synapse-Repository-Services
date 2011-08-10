package org.sagebionetworks.repo.model.jdo;

import java.util.List;
import java.util.Map;

import javax.jdo.JDOException;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import org.springframework.orm.jdo.JdoCallback;
import org.springframework.orm.jdo.JdoTemplate;

@SuppressWarnings("unchecked")
public class JDOExecutor {
	private JdoTemplate jdoTemplate;
	
	public JDOExecutor(JdoTemplate jdoTemplate) {
		this.jdoTemplate = jdoTemplate;
	}
	
	// call when sql specifies just one col to return
	public List<Object> executeSingleCol(final String sql) {
		return (List<Object>) jdoTemplate.execute(new JdoCallback<List<Object>>() {
			@Override
			public List<Object> doInJdo(PersistenceManager pm) throws JDOException {
				Query query = pm.newQuery("javax.jdo.query.SQL", sql);
				List<Object> ans = (List<Object>)query.execute();
				return ans;
			}
		});
	}
	
	// call when sql specifies just one col to return
	public List<Object> executeSingleCol(final String sql, final Map<String,Object> parameters ) {
		return (List<Object>) jdoTemplate.execute(new JdoCallback<List<Object>>() {
			@Override
			public List<Object> doInJdo(PersistenceManager pm) throws JDOException {
				Query query = pm.newQuery("javax.jdo.query.SQL", sql);
				List<Object> ans = (List<Object>)query.executeWithMap(parameters);
				return ans;
			}
		});
	}
	
	// call when sql specifies multiple col's to return
	public List<Object[]> execute(final String sql) {
		return (List<Object[]>) jdoTemplate.execute(new JdoCallback<List<Object[]>>() {
			@Override
			public List<Object[]> doInJdo(PersistenceManager pm) throws JDOException {
				Query query = pm.newQuery("javax.jdo.query.SQL", sql);
				List<Object[]> ans = (List<Object[]>)query.execute();
				return ans;
			}
		});
	}
	
	// call when sql specifies multiple col's to return
	public List<Object[]> execute(final String sql, final Map<String,Object> parameters ) {
		return (List<Object[]>) jdoTemplate.execute(new JdoCallback<List<Object[]>>() {
			@Override
			public List<Object[]> doInJdo(PersistenceManager pm) throws JDOException {
				Query query = pm.newQuery("javax.jdo.query.SQL", sql);
				List<Object[]> ans = (List<Object[]>)query.executeWithMap(parameters);
				return ans;
			}
		});
	}
	

	public <T> List<T> execute(final Class<T> t, final String filter,
			final String params,
			final String vars) {
		return (List<T>) jdoTemplate.execute(new JdoCallback<List<T>>() {
			@Override
			public List<T> doInJdo(PersistenceManager pm) throws JDOException {
				Query query = pm.newQuery(t);
				if (filter!=null) query.setFilter(filter);
				if (vars!=null) query.declareVariables(vars);
				if (params!=null) query.declareParameters(params);
				List<T> ans = (List<T>)query.execute();
				return ans;
			}
		});
	}
	
	public <T> List<T> execute(final Class<T> t, final String filter,
			final String params,
			final String vars,
			final long fromIncl,
			final long toExcl,
			final String ordering) {
		return execute(t, filter, params, vars, fromIncl, toExcl, ordering, true);
	}

	public <T> List<T> execute(final Class<T> t, final String filter,
			final String params,
			final String vars,
			final long fromIncl,
			final long toExcl,
			final String ordering,
			final boolean ascending) {
		return (List<T>) jdoTemplate.execute(new JdoCallback<List<T>>() {
			@Override
			public List<T> doInJdo(PersistenceManager pm) throws JDOException {
				Query query = pm.newQuery(t);
				if (filter!=null) query.setFilter(filter);
				if (vars!=null) query.declareVariables(vars);
				if (params!=null) query.declareParameters(params);
				query.setRange(fromIncl, toExcl);
				query.setOrdering(ordering+ (ascending?" ascending":" descending"));
				List<T> ans = (List<T>)query.execute();
				return ans;
			}
		});
	}
	

	
	public <T> List<T> execute(final Class<T> t, final String filter,
			final String params,
			final String vars,
			final Object arg) {
		return (List<T>) jdoTemplate.execute(new JdoCallback<List<T>>() {
			@Override
			public List<T> doInJdo(PersistenceManager pm) throws JDOException {
				Query query = pm.newQuery(t);
				if (filter!=null) query.setFilter(filter);
				if (vars!=null) query.declareVariables(vars);
				if (params!=null) query.declareParameters(params);
				List<T> ans = (List<T>)query.execute(arg);
				return ans;
			}
		});
	}
	
	public <T> List<T> execute(final Class<T> t, final String filter,
			final String params,
			final String vars,
			final Object arg1,
			final Object arg2) {
		return (List<T>) jdoTemplate.execute(new JdoCallback<List<T>>() {
			@Override
			public List<T> doInJdo(PersistenceManager pm) throws JDOException {
				Query query = pm.newQuery(t);
				if (filter!=null) query.setFilter(filter);
				if (vars!=null) query.declareVariables(vars);
				if (params!=null) query.declareParameters(params);
				List<T> ans = (List<T>)query.execute(arg1, arg2);
				return ans;
			}
		});
	}
	
	public <T> List<T> execute(final Class<T> t, final String filter,
			final String params,
			final String vars,
			final Object arg1,
			final Object arg2,
			final Object arg3) {
		return (List<T>) jdoTemplate.execute(new JdoCallback<List<T>>() {
			@Override
			public List<T> doInJdo(PersistenceManager pm) throws JDOException {
				Query query = pm.newQuery(t);
				if (filter!=null) query.setFilter(filter);
				if (vars!=null) query.declareVariables(vars);
				if (params!=null) query.declareParameters(params);
				List<T> ans = (List<T>)query.execute(arg1, arg2, arg3);
				return ans;
			}
		});
	}
	

}
