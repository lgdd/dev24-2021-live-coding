FROM liferay/portal:7.4.3.4-ga4

ENV JAVA_VERSION=zulu8

ARG TARGET_ENV=prod

COPY --chown=liferay:liferay build/docker/deploy /mnt/liferay/deploy
COPY --chown=liferay:liferay build/docker/configs/$TARGET_ENV /mnt/liferay/files
