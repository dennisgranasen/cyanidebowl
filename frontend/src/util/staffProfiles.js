export const staffProfilePath = (profile) => {
  if (!profile?.id) return '/staff';
  const id = encodeURIComponent(profile.id);
  return profile.profileType === 'HUMAN' ? `/staff/user/${id}` : `/staff/${id}`;
};

export const staffProfileUpdatePayload = (profile = {}) => ({
  displayName: profile.displayName ?? '',
  avatarUrl: profile.avatarUrl ?? '',
  portraitUrl: profile.portraitUrl ?? '',
  bio: profile.bio ?? '',
});

export const toStaffCards = (ai = [], humans = [], humanRole = 'Staff') => [
  ...(ai || []).map((profile) => ({
    ...profile,
    profileType: 'AI',
    displayName: profile.displayName || profile.alias || profile.id || 'AI reporter',
    avatarImage: profile.avatarImage || profile.portraitImage || null,
    portraitImage: profile.portraitImage || profile.avatarImage || null,
    role: profile.role || profile.category || 'Staff reporter',
    summary: profile.summary || profile.role || profile.category || null,
    profileUrl: staffProfilePath({ ...profile, profileType: 'AI' }),
  })),
  ...(humans || []).map((profile) => ({
    ...profile,
    profileType: 'HUMAN',
    displayName: profile.displayName || 'Staff',
    alias: profile.displayName || 'Staff',
    avatarImage: profile.avatarUrl || profile.portraitUrl || null,
    portraitImage: profile.portraitUrl || profile.avatarUrl || null,
    role: humanRole,
    summary: profile.bio || null,
    profileUrl: staffProfilePath({ ...profile, profileType: 'HUMAN' }),
  })),
];
