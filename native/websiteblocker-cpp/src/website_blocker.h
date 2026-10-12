#pragma once

#include <cstdint>
#include <string>
#include <string_view>
#include <vector>

namespace websiteblocker {

constexpr uint32_t kMagic = 0x314C4257;
constexpr uint32_t kVersion = 1;

struct FileHeader {
    uint32_t magic;
    uint32_t version;
    uint64_t domainCount;
    uint64_t checksum;
};

uint64_t HashDomain(std::string_view domain);
std::string NormalizeDomain(const std::string& raw);
bool IsBoilerplateHost(const std::string& domain);

class Builder {
public:
    struct Stats {
        uint64_t added = 0;
        uint64_t skipped = 0;
    };

    Stats AddText(const std::string& text);
    bool Finalize(const std::string& outputPath, uint64_t* outDomainCount, uint64_t* outSizeBytes);

private:
    std::vector<uint64_t> hashes_;
};

class Database {
public:
    ~Database();
    static Database* Load(const std::string& path);
    bool IsBlocked(const std::string& host) const;
    uint64_t DomainCount() const { return domainCount_; }

private:
    void* mapping_ = nullptr;
    size_t mappingSize_ = 0;
    const uint64_t* hashes_ = nullptr;
    uint64_t domainCount_ = 0;
    bool ContainsHash(uint64_t hash) const;
};

}  // namespace websiteblocker
