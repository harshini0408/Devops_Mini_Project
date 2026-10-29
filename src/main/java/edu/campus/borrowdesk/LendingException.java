package edu.campus.borrowdesk;

/** A lending rule was broken; the message is safe to show to the operator. */
public class LendingException extends RuntimeException {

	public LendingException(String message) {
		super(message);
	}
}
