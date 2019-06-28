package ntx.note.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;

/**
 * The account for http://quill.sagepad.org
 * @author vbraun
 *
 */
public class NoteAccount {
	private final static String TAG = "NoteAccount";
	
	public final static String ACCOUNT_TYPE = "ntx.note.sync.account";

	public final static String EXTRA_NAME = "extra_name";
	public final static String EXTRA_EMAIL_ADDRESS = "extra_email_address";
	public final static String EXTRA_PASSWORD = "extra_password";

	protected final AccountManager accountManager;
	protected final Account account;
	
	public NoteAccount(Context context) {
		accountManager = AccountManager.get(context);
		Account accountsList[] = accountManager.getAccountsByType(ACCOUNT_TYPE);
		if (accountsList.length > 0)
			account = accountsList[0];
		else
			account = null;
	}
	
	/**
	 * Whether an account is set up
	 * @return
	 */
	public boolean exists() {
		return (account != null);
	}
	
	public String name() {
		String result = accountManager.getUserData(account, EXTRA_NAME);
		if (result == null)
			return "Unknown name";
		return result;
	}
	
	public String email() {
		String result = accountManager.getUserData(account, EXTRA_EMAIL_ADDRESS);
		if (result == null)
			return "Email address not known";
		return result;
	}
	
	public String password() {
		String result = accountManager.getPassword(account);
		if (result == null)
			return "******";
		return result;
	}

	public boolean equals(NoteAccount other) {
		return (name().equals(other.name()) && 
				email().equals(other.email()) &&
				password().equals(other.password()));
	}
}
