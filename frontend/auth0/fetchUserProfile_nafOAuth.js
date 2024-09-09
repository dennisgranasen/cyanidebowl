/*

function(accessToken, ctx, oauth2Callback) {
    function callback(error, response, blob) {
        if (error) return oauth2Callback(error);
        if (response.statusCode !== 200) return oauth2Callback(new Error('StatusCode: ' + response.statusCode));
        const nafProfile = JSON.parse(blob);
        oauth2Callback(null, nafProfile);
    };
    request.get('https://member.thenaf.net/index.php?module=NAF&type=oauthendpoint', {
        headers: {
            'authorization': 'Bearer ' + accessToken
        }
    }, callback);
}

*/
