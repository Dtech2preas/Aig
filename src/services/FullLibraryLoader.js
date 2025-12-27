import anime_hash from '../../assets/data/anime_index/anime_hash.json';
import anime_A from '../../assets/data/anime_index/anime_A.json';
import anime_B from '../../assets/data/anime_index/anime_B.json';
import anime_C from '../../assets/data/anime_index/anime_C.json';
import anime_D from '../../assets/data/anime_index/anime_D.json';
import anime_E from '../../assets/data/anime_index/anime_E.json';
import anime_F from '../../assets/data/anime_index/anime_F.json';
import anime_G from '../../assets/data/anime_index/anime_G.json';
import anime_H from '../../assets/data/anime_index/anime_H.json';
import anime_I from '../../assets/data/anime_index/anime_I.json';
import anime_J from '../../assets/data/anime_index/anime_J.json';
import anime_K from '../../assets/data/anime_index/anime_K.json';
import anime_L from '../../assets/data/anime_index/anime_L.json';
import anime_M from '../../assets/data/anime_index/anime_M.json';
import anime_N from '../../assets/data/anime_index/anime_N.json';
import anime_O from '../../assets/data/anime_index/anime_O.json';
import anime_P from '../../assets/data/anime_index/anime_P.json';
import anime_Q from '../../assets/data/anime_index/anime_Q.json';
import anime_R from '../../assets/data/anime_index/anime_R.json';
import anime_S from '../../assets/data/anime_index/anime_S.json';
import anime_T from '../../assets/data/anime_index/anime_T.json';
import anime_U from '../../assets/data/anime_index/anime_U.json';
import anime_V from '../../assets/data/anime_index/anime_V.json';
import anime_W from '../../assets/data/anime_index/anime_W.json';
import anime_X from '../../assets/data/anime_index/anime_X.json';
import anime_Y from '../../assets/data/anime_index/anime_Y.json';
import anime_Z from '../../assets/data/anime_index/anime_Z.json';
import anime_U_umlaut from '../../assets/data/anime_index/anime_U_umlaut.json';
import anime_O_macron from '../../assets/data/anime_index/anime_O_macron.json';

const allParts = [
  anime_hash,
  anime_A, anime_B, anime_C, anime_D, anime_E, anime_F, anime_G,
  anime_H, anime_I, anime_J, anime_K, anime_L, anime_M, anime_N,
  anime_O, anime_P, anime_Q, anime_R, anime_S, anime_T, anime_U,
  anime_V, anime_W, anime_X, anime_Y, anime_Z,
  anime_U_umlaut, anime_O_macron
];

export const getFullLibrary = () => {
  let allAnime = [];
  for (const part of allParts) {
    if (part && part.anime) {
      allAnime = allAnime.concat(part.anime);
    }
  }
  return allAnime;
};
