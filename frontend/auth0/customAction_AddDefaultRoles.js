/**
 * Handler that will be called during the execution of a PostLogin Flow.
 *
 * @param {Event} event - Details about the user and the context in which they are logging in.
 * @param {PostLoginAPI} api - Interface whose methods can be used to change the behavior of the login.
 */
exports.onExecutePostLogin = async (event, api) => {

    // Check if the user has a role assigned
    if (event.authorization && event.authorization.roles && event.authorization.roles.length > 0) {
        return;
    }

    // Create management API client instance
    const ManagementClient = require("auth0").ManagementClient;

    const management = new ManagementClient({
        domain: event.secrets.domain,
        clientId: event.secrets.clientId,
        clientSecret: [REDACTED]
        audience: 'https://warp-scores.eu.auth0.com/api/v2/',
    });

    const params =  { id : event.user.user_id };
    const data = { "roles" : ["rol_qThYnqOJScNa6eKo"] };

    try {
        await management.users.assignRoles(params, data);
    } catch (e) {
        console.log(e);
    }
};
