import config from './config';

function addOpusParam(url, opus) {
  console.log('addOpusParam', url, opus);
  if (opus !== undefined && opus !== null) {
    const sep = url.includes('?') ? '&' : '?';
    return `${url}${sep}opus=${opus}`;
  }
  return url;
}

export default {
  stadium: (name, opus) => addOpusParam(`${config.backendUrl}/img/stadium/${name}`,opus),
  logo: (name, opus) => addOpusParam(`${config.backendUrl}/img/logo/${name}`,opus),
  race: (name, opus) => addOpusParam(`${config.backendUrl}/img/race/${name}`,opus),
  skill: (name, opus) => addOpusParam(`${config.backendUrl}/img/skill/${name}`,opus),
  warpscoresLogoPng: (size = null) => `${config.backendUrl}/img/warpscores.png${size ? `/${size}` : ''}`,
  warpscoresLogoSvg: () => `${config.backendUrl}/img/warpscores.svg`,
  dbbcLogoPng: (size = null) => `${config.backendUrl}/img/dbbc.png${size ? `/${size}` : ''}`,
};
