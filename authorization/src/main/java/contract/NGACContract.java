package contract;

import ngac.BlossomPDP;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.Transaction;

@Contract(
		name = "ngac",
		info = @Info(
				title = "Blossom Authorization NGAC Contract",
				description = "Functions to provide information regarding the NGAC policy",
				version = "0.0.1"
		)
)
public class NGACContract implements ContractInterface {

	/**
	 * Get all the roles recognized by the NGAC policy. This is not all the attributes a user may have in NGAC, just
	 * the roles. This is also not necessarily the roles the requesting user has.
	 * @return The list of all roles.
	 */
	@Transaction
	public String[] GetAllRoles(Context ctx) {
		return new BlossomPDP().getAllRoles();
	}

	/**
	 * Get all the privileges available to users in the NGAC policy. This is not the privileges the requesting user has.
	 * @return The list of all privileges.
	 */
	@Transaction
	public String[] GetAllPrivileges(Context ctx) {
		return new BlossomPDP().getAllPrivileges();
	}

}
