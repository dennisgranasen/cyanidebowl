import config from './config';

export default {
  stadium: (name) => `${config.backendUrl}/img/stadium/${name}`,
  logo: (name) => `${config.backendUrl}/img/logo/${name}`,
  race: (name) => `${config.backendUrl}/img/race/${name}`,
  skill: (name) => `${config.backendUrl}/img/skill/${name}`,
  warpscoresLogoPng: (size = null) => `${config.backendUrl}/img/warpscores.png${size ? `/${size}` : ''}`,
  warpscoresLogoSvg: () => `${config.backendUrl}/img/warpscores.svg`,
};
