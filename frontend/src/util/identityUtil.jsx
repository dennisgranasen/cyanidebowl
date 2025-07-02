export const identityUtils = {
    opus: (id) => {
        if (!id) {
            console.warn('No opus provided for identity:', id);
            return 3; // Default opus
        }
        if (typeof(id) === 'string') {
            return id.split('_')[0] ? parseInt(id.split('_')[0]) || 3 : 3;
        }
        return id.opus || 3
    },
    value: (id) => {
        if (!id) return null;
        if (typeof(id) === 'string') {
            const parts = id.split('_');
            return parts.length >= 2 ? parts.slice(1).join('_') : id;
        }
        return id.value || id;
    },
    key: (id) => {
        if (!id) return null;
        if (typeof(id) === 'string') {
            return id;
        }
        return id.key || id;
    },
    parts: (id) => {
        if (!id) return [];
        if (typeof(id) === 'string') {
            const parts = id.split('_');
            return parts.length > 1 ? parts.slice(1) : [id];
        }
        return id.parts || [id.value] || id;
    }
};