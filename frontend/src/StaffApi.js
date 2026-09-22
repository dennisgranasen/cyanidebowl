import axios from 'axios';
import config from './config';
axios.defaults.baseURL = config.backendUrl;
const authorizationParams = { authorizationParams: { audience: config.auth0Audience } };
async function headers(silent, popup) {
  if (!config.isProduction) return { withCredentials: true, headers: { Authorization: 'Bearer dev-token' } };
  let token; try { token = await silent(authorizationParams); } catch (e) { token = await popup(authorizationParams); }
  return { withCredentials: true, headers: { Authorization: `Bearer ${token}` } };
}
export default {
  users: async () => (await axios.get('/staff/users')).data,
  user: async id => (await axios.get(`/staff/users/${encodeURIComponent(id)}`)).data,
  ownProfile: async (silent,popup) => (await axios.get('/user/staff-profile', await headers(silent,popup))).data,
  updateOwnProfile: async (data,silent,popup) => (await axios.put('/user/staff-profile', data, await headers(silent,popup))).data,
};
