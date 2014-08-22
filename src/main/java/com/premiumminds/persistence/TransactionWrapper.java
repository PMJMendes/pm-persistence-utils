package com.premiumminds.persistence;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Usage example:
 * 
 * Integer result = new TransactionWrapper<Integer>() {
 * 
 *		public abstract Integer runTransaction() throws Throwable {
 *			...
 *			your transaction code here
 *			...
 *		}
 *
 * }.execute(); //Execute the transaction
 *
 */
public abstract class TransactionWrapper<T> {

	private static final Logger LOGGER = LoggerFactory.getLogger(TransactionWrapper.class);
	
	protected EntityManager em;
	private boolean wasActive;

	/**
	 * The TransactionWrapper constructor
	 * @param em The entity manager managing the transaction.
	 */
	@Inject
	public TransactionWrapper(EntityManager em) {
		this.em = em;
	} 

	private void setupTransaction() {
		this.wasActive = em.getTransaction().isActive();
		if(!wasActive) {
			em.getTransaction().begin();
			LOGGER.trace(String.format("Begin Transaction - EM : %s", em));
		} else {
			em.flush();
			LOGGER.trace(String.format("Flush while entering - EM : %s", em));
		}
	}

	/**
	 * Runs the transaction instructions
	 * @return The transaction return value
	 * @throws Exception an exception wrapping all thrown exceptions in the runTransaction block
	 */
	public abstract T runTransaction() throws Throwable;

	/**
	 * Executes the transaction wrapping steps
	 * @return The transaction return value
	 * @throws Throwable 
	 * @throws Exception 
	 */
	public T execute() throws Throwable {
		setupTransaction();
		T result = null;
		try {
			result = runTransaction();
		} catch(Throwable t) {
			em.getTransaction().setRollbackOnly();
			LOGGER.trace(String.format("Set for Rollback - EM : %s and Exception : %s", em, t));
			finalizeTransaction();
			throw t;
		}
		try {
			finalizeTransaction();
		} catch(Throwable t) {
			throw t;
		}
		return result;
	}

	/**
	 * Sets the transaction to be commited
	 */
	public void commit() {
		em.getTransaction().commit();
	}

	/**
	 * Sets the transaction for rollback
	 */
	public void rollback() {
		em.getTransaction().setRollbackOnly();
	}

	private void finalizeTransaction() {
		if(!wasActive) {
			if(em.getTransaction().getRollbackOnly()) {
				em.getTransaction().rollback();
				LOGGER.trace(String.format("Rollback - EM : %s", em));
			} else {
				em.getTransaction().commit();
				LOGGER.trace(String.format("Commit - EM : %s", em));
			}
		}
		if(em.getTransaction().isActive()) {
			em.flush();
			LOGGER.trace(String.format("Flush on exit - EM : %s", em));
		}
	}

}