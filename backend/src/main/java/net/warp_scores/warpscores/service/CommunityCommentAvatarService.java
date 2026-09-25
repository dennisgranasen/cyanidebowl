package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.domain.persistence.AiCommunityMemberProfileRepository;
import net.warp_scores.warpscores.domain.persistence.WarpScoresUserRepository;
import net.warp_scores.warpscores.model.AiCommunityMemberProfile;
import net.warp_scores.warpscores.model.CommunityComment;
import net.warp_scores.warpscores.model.WarpScoresUser;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CommunityCommentAvatarService {
    private final WarpScoresUserRepository users;
    private final AiCommunityMemberProfileRepository profiles;

    public List<CommunityComment> attach(List<CommunityComment> comments) {
        List<Long> userIds = comments.stream()
                .map(CommunityComment::getAuthorUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, WarpScoresUser> usersById = new HashMap<>();
        users.findAllById(userIds).forEach(user -> usersById.put(user.getId(), user));

        Map<Long, String> fanAvatars = new HashMap<>();
        profiles.findByUserIdIn(userIds).forEach(profile -> {
            String avatar = StringUtils.hasText(profile.getAvatarImageUrl())
                    ? profile.getAvatarImageUrl()
                    : profile.getProfileImageUrl();
            if (StringUtils.hasText(avatar)) fanAvatars.put(profile.getUserId(), avatar);
        });

        comments.forEach(comment -> {
            String avatar = fanAvatars.get(comment.getAuthorUserId());
            WarpScoresUser user = usersById.get(comment.getAuthorUserId());
            if (!StringUtils.hasText(avatar) && user != null) avatar = user.getPublicAvatarUrl();
            comment.setAuthorAvatarUrl(avatar);
        });
        return comments;
    }
}