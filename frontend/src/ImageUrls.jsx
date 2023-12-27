import config from './config';

export default {
  stadium: (name) => `${config.backendUrl}/img/stadium/${name}`,
  logo: (name) => `${config.backendUrl}/img/logo/${name}`,
  race: (name) => `${config.backendUrl}/img/race/${name}`,
  skill: (name) => `${config.backendUrl}/img/skill/${name}`,
};
