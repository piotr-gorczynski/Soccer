'use strict';
const { facebookCommunityUrl } = require('./community.json');
function facebookUrl(value, postOnly = false) {
  if (typeof value !== 'string' || value.length > 512 || /[\s\\\u0000-\u001f]/u.test(value)) return null;
  try {
    const u = new URL(value);
    if (u.protocol !== 'https:' || u.username || u.password || u.port || u.hash
        || !['www.facebook.com', 'facebook.com', 'm.facebook.com'].includes(u.hostname)) return null;
    const post = /^\/[A-Za-z0-9._-]+\/posts\/[A-Za-z0-9._-]+\/?$/.test(u.pathname)
      || (u.pathname === '/permalink.php' && /^[A-Za-z0-9]+$/.test(u.searchParams.get('story_fbid') || '')
        && /^\d+$/.test(u.searchParams.get('id') || ''));
    if (postOnly && !post) return null;
    if (!post && u.href !== facebookCommunityUrl) return null;
    // Drop tracking/unknown query fields rather than publishing arbitrary data.
    if (post) {
      const story = u.searchParams.get('story_fbid'), page = u.searchParams.get('id');
      u.search = '';
      if (u.pathname === '/permalink.php') {
        u.searchParams.set('story_fbid', story); u.searchParams.set('id', page);
      }
    }
    return u.href;
  } catch { return null; }
}
function destination(tournament) {
  const configured = tournament.postPayoutFacebookPostUrl;
  if (configured == null || configured === '') return { url: facebookCommunityUrl, type: 'community' };
  const url = facebookUrl(configured, true);
  if (!url) throw Error('Invalid tournament Facebook post URL.');
  return { url, type: 'tournament_post' };
}
function publicFeedback(data) {
  if (data.authorType !== 'support' || data.kind !== 'post_payout_feedback') return {};
  const url = facebookUrl(data.feedbackUrl, data.feedbackDestinationType === 'tournament_post');
  if (!url || !['community', 'tournament_post'].includes(data.feedbackDestinationType)
      || (data.feedbackDestinationType === 'community' && url !== facebookCommunityUrl)) return {};
  return { kind: 'post_payout_feedback', feedbackUrl: url, destinationType: data.feedbackDestinationType };
}
module.exports = { facebookUrl, destination, publicFeedback };
