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
    },
    combine: (id1, id2) => {
        if (!id1 || !id2) {
            console.warn('Cannot combine identities, one or both are missing:', id1, id2);
            return null;
        }
        const opus1 = identityUtils.opus(id1);
        const opus2 = identityUtils.opus(id2);

        if (opus1 !== opus2) {
            console.warn('Cannot combine identities, different opuses:', id1, id2);
            return null;
        }
        const value1 = identityUtils.value(id1);
        let value2 = identityUtils.value(id2).split('_');
        value2 = value2[value2.length - 1]; // Get the last part of value2

        return `${opus1}_${value1}_${value2}`;
    }
};